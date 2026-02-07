package com.logistics.track17.dto;

import lombok.Data;
import java.util.*;

/**
 * Section文件变更DTO
 */
@Data
public class SectionFileChanges {

    /** 删除的sections */
    private List<String> deleted = new ArrayList<>();

    /** 新增的sections */
    private List<String> added = new ArrayList<>();

    /** 重命名的sections (oldName -> newName) */
    private Map<String, String> renamed = new HashMap<>();

    /** 修改的sections */
    private List<String> modified = new ArrayList<>();

    public void addDeleted(String sectionName) {
        deleted.add(sectionName);
    }

    public void addAdded(String sectionName) {
        added.add(sectionName);
    }

    public void addRenamed(String oldName, String newName) {
        renamed.put(oldName, newName);
    }

    public void addModified(String sectionName) {
        modified.add(sectionName);
    }

    public boolean hasChanges() {
        return !deleted.isEmpty() || !added.isEmpty() ||
                !renamed.isEmpty() || !modified.isEmpty();
    }
}
