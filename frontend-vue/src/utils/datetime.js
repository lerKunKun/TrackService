import dayjs from 'dayjs'

/**
 * 格式化日期时间
 * @param {string|Date} datetime - 日期时间
 * @param {string} format - 格式化模板，默认 'YYYY-MM-DD HH:mm:ss'
 * @returns {string} 格式化后的日期时间字符串
 */
export function formatDateTime(datetime, format = 'YYYY-MM-DD HH:mm:ss') {
    if (!datetime) return '-'
    return dayjs(datetime).format(format)
}

/**
 * 格式化日期
 * @param {string|Date} date - 日期
 * @returns {string} 格式化后的日期字符串
 */
export function formatDate(date) {
    return formatDateTime(date, 'YYYY-MM-DD')
}

/**
 * 格式化时间
 * @param {string|Date} time - 时间
 * @returns {string} 格式化后的时间字符串
 */
export function formatTime(time) {
    return formatDateTime(time, 'HH:mm:ss')
}

/**
 * 相对时间（如：3分钟前）
 * @param {string|Date} datetime - 日期时间
 * @returns {string} 相对时间描述
 */
export function fromNow(datetime) {
    if (!datetime) return '-'
    return dayjs(datetime).fromNow()
}

export default {
    formatDateTime,
    formatDate,
    formatTime,
    fromNow
}
