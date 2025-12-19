package com.logistics.track17.service;

import com.logistics.track17.dto.SectionFileChanges;
import com.logistics.track17.dto.ThemeDiffResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 主题Git Diff分析器
 * 使用JGit分析两个主题版本的差异
 */
@Slf4j
@Service
public class ThemeGitDiffAnalyzer {

    /**
     * 分析两个主题ZIP的差异
     */
    public ThemeDiffResult analyze(Path oldThemeZip, Path newThemeZip) throws Exception {
        log.info("Starting Git diff analysis...");

        // 1. 解压主题
        Path oldTheme = unzipTheme(oldThemeZip);
        Path newTheme = unzipTheme(newThemeZip);

        // 2. 初始化Git仓库进行对比
        Path gitRepo = initGitRepository(oldTheme, newTheme);

        try (Git git = Git.open(gitRepo.toFile())) {
            // 3. 执行git diff
            List<DiffEntry> diffs = execGitDiff(git);

            // 4. 解析差异
            SectionFileChanges changes = parseDiffEntries(diffs);

            // 5. 构建结果
            ThemeDiffResult result = ThemeDiffResult.builder()
                    .sectionChanges(changes)
                    .build();

            log.info("Git diff analysis completed. Found {} changes",
                    changes.hasChanges() ? "some" : "no");

            return result;

        } finally {
            // 清理临时文件
            cleanupTempFiles(gitRepo, oldTheme, newTheme);
        }
    }

    /**
     * 解压主题ZIP
     */
    private Path unzipTheme(Path zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("theme-");

        // 使用Apache Commons Compress解压
        org.apache.commons.compress.archivers.zip.ZipFile zip = new org.apache.commons.compress.archivers.zip.ZipFile(
                zipFile.toFile());

        // Java 8兼容：使用Enumeration的传统遍历方式
        java.util.Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entries = zip.getEntries();

        while (entries.hasMoreElements()) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry = entries.nextElement();
            try {
                File file = tempDir.resolve(entry.getName()).toFile();
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    Files.copy(zip.getInputStream(entry), file.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                log.error("Failed to extract: {}", entry.getName(), e);
            }
        }

        zip.close();
        log.debug("Unzipped theme to: {}", tempDir);
        return tempDir;
    }

    /**
     * 初始化Git仓库
     */
    private Path initGitRepository(Path oldTheme, Path newTheme) throws Exception {
        Path tempRepo = Files.createTempDirectory("theme-git-");

        try (Git git = Git.init().setDirectory(tempRepo.toFile()).call()) {
            // Commit 1: 旧版本
            FileUtils.copyDirectory(oldTheme.toFile(), tempRepo.toFile());
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Old version").call();

            // 清空目录（保留.git）
            cleanDirectoryExceptGit(tempRepo);

            // Commit 2: 新版本
            FileUtils.copyDirectory(newTheme.toFile(), tempRepo.toFile());
            git.add().addFilepattern(".").setUpdate(true).call(); // 标记删除
            git.add().addFilepattern(".").call(); // 添加新文件
            git.commit().setMessage("New version").call();
        }

        log.debug("Initialized Git repo at: {}", tempRepo);
        return tempRepo;
    }

    /**
     * 执行git diff
     */
    private List<DiffEntry> execGitDiff(Git git) throws Exception {
        // 获取最近两个commit
        Iterable<org.eclipse.jgit.revwalk.RevCommit> commits = git.log().setMaxCount(2).call();
        java.util.ArrayList<org.eclipse.jgit.revwalk.RevCommit> commitList = new java.util.ArrayList<org.eclipse.jgit.revwalk.RevCommit>();
        commits.forEach(commitList::add);

        if (commitList.size() < 2) {
            throw new RuntimeException("Not enough commits for diff");
        }

        org.eclipse.jgit.revwalk.RevCommit newCommit = commitList.get(0);
        org.eclipse.jgit.revwalk.RevCommit oldCommit = commitList.get(1);

        org.eclipse.jgit.lib.Repository repo = git.getRepository();
        org.eclipse.jgit.lib.ObjectReader reader = repo.newObjectReader();

        org.eclipse.jgit.treewalk.CanonicalTreeParser oldTree = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
        oldTree.reset(reader, oldCommit.getTree());

        org.eclipse.jgit.treewalk.CanonicalTreeParser newTree = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
        newTree.reset(reader, newCommit.getTree());

        // 分析全量文件，不仅限于sections目录
        List<DiffEntry> diffs = git.diff()
                .setOldTree(oldTree)
                .setNewTree(newTree)
                // 移除PathFilter，分析所有文件以检测所有liquid变化
                .call();

        reader.close();

        log.info("Found {} changes across all theme files", diffs.size());
        return diffs;
    }

    /**
     * 解析DiffEntry - 只处理sections目录的变化
     */
    private SectionFileChanges parseDiffEntries(List<DiffEntry> diffs) {
        SectionFileChanges changes = new SectionFileChanges();

        for (DiffEntry diff : diffs) {
            String oldPath = diff.getOldPath();
            String newPath = diff.getNewPath();

            // 只处理sections/目录的文件
            boolean isOldInSections = oldPath.startsWith("sections/");
            boolean isNewInSections = newPath.startsWith("sections/");

            // 如果两个路径都不在sections/目录，跳过
            if (!isOldInSections && !isNewInSections) {
                continue;
            }

            switch (diff.getChangeType()) {
                case DELETE:
                    if (isOldInSections) {
                        String deletedSection = extractSectionName(oldPath);
                        changes.addDeleted(deletedSection);
                        log.debug("DELETE: {}", deletedSection);
                    }
                    break;

                case ADD:
                    if (isNewInSections) {
                        String addedSection = extractSectionName(newPath);
                        changes.addAdded(addedSection);
                        log.debug("ADD: {}", addedSection);
                    }
                    break;

                case RENAME:
                    // RENAME必须两个路径都在sections/才处理
                    if (isOldInSections && isNewInSections) {
                        String oldName = extractSectionName(oldPath);
                        String newName = extractSectionName(newPath);
                        changes.addRenamed(oldName, newName);
                        log.debug("RENAME: {} -> {}", oldName, newName);
                    }
                    break;

                case MODIFY:
                    if (isNewInSections) {
                        String modifiedSection = extractSectionName(newPath);
                        changes.addModified(modifiedSection);
                        log.debug("MODIFY: {}", modifiedSection);
                    }
                    break;

                default:
                    break;
            }
        }

        log.info("Section changes - Added: {}, Deleted: {}, Renamed: {}, Modified: {}",
                changes.getAdded().size(),
                changes.getDeleted().size(),
                changes.getRenamed().size(),
                changes.getModified().size());

        return changes;
    }

    /**
     * 提取Section名称
     * sections/product-hero.liquid -> product-hero
     */
    private String extractSectionName(String path) {
        return path.replace("sections/", "")
                .replace(".liquid", "");
    }

    /**
     * 清空目录但保留.git
     */
    private void cleanDirectoryExceptGit(Path dir) throws Exception {
        Files.list(dir)
                .filter(path -> !path.getFileName().toString().equals(".git"))
                .forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            FileUtils.deleteDirectory(path.toFile());
                        } else {
                            Files.delete(path);
                        }
                    } catch (Exception e) {
                        log.error("Failed to delete: {}", path, e);
                    }
                });
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(Path... paths) {
        for (Path path : paths) {
            try {
                if (path != null && Files.exists(path)) {
                    FileUtils.deleteDirectory(path.toFile());
                    log.debug("Cleaned up: {}", path);
                }
            } catch (Exception e) {
                log.warn("Failed to cleanup: {}", path, e);
            }
        }
    }
}
