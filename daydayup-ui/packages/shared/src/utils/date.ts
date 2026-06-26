import dayjs, { Dayjs } from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

/**
 * 格式化日期
 */
export function formatDate(date: string | Date | Dayjs, format = 'YYYY-MM-DD HH:mm:ss'): string {
  return dayjs(date).format(format);
}

/**
 * 解析日期字符串
 */
export function parseDate(dateString: string): Dayjs {
  return dayjs(dateString);
}

/**
 * 获取相对时间（如：3天前）
 */
export function getRelativeTime(date: string | Date | Dayjs): string {
  return dayjs(date).fromNow();
}

/**
 * 获取时间戳
 */
export function getTimestamp(date?: string | Date | Dayjs): number {
  return date ? dayjs(date).valueOf() : dayjs().valueOf();
}

/**
 * 是否是今天
 */
export function isToday(date: string | Date | Dayjs): boolean {
  return dayjs(date).isSame(dayjs(), 'day');
}

/**
 * 是否是昨天
 */
export function isYesterday(date: string | Date | Dayjs): boolean {
  return dayjs(date).isSame(dayjs().subtract(1, 'day'), 'day');
}
