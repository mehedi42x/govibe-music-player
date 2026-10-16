import type { RepeatMode } from '../domain/models';

export interface QueueState {
  trackIds: string[];
  currentIndex: number;
  shuffle: boolean;
  repeat: RepeatMode;
}

export function createQueue(trackIds: string[] = [], currentIndex = -1): QueueState {
  return {
    trackIds: [...trackIds],
    currentIndex: trackIds.length ? Math.max(0, Math.min(currentIndex, trackIds.length - 1)) : -1,
    shuffle: false,
    repeat: 'off',
  };
}

export function enqueue(queue: QueueState, ids: string[]): QueueState {
  const known = new Set(queue.trackIds);
  return { ...queue, trackIds: [...queue.trackIds, ...ids.filter((id) => !known.has(id))] };
}

export function removeFromQueue(queue: QueueState, index: number): QueueState {
  if (index < 0 || index >= queue.trackIds.length) return queue;
  const trackIds = queue.trackIds.filter((_, i) => i !== index);
  let currentIndex = queue.currentIndex;
  if (!trackIds.length) currentIndex = -1;
  else if (index < currentIndex) currentIndex -= 1;
  else if (index === currentIndex) currentIndex = Math.min(index, trackIds.length - 1);
  return { ...queue, trackIds, currentIndex };
}

export function moveInQueue(queue: QueueState, from: number, to: number): QueueState {
  if (from < 0 || to < 0 || from >= queue.trackIds.length || to >= queue.trackIds.length || from === to)
    return queue;
  const trackIds = [...queue.trackIds];
  const [moved] = trackIds.splice(from, 1);
  if (moved === undefined) return queue;
  trackIds.splice(to, 0, moved);
  let currentIndex = queue.currentIndex;
  if (currentIndex === from) currentIndex = to;
  else if (from < currentIndex && to >= currentIndex) currentIndex -= 1;
  else if (from > currentIndex && to <= currentIndex) currentIndex += 1;
  return { ...queue, trackIds, currentIndex };
}

export function setCurrent(queue: QueueState, trackId: string): QueueState {
  const existing = queue.trackIds.indexOf(trackId);
  if (existing >= 0) return { ...queue, currentIndex: existing };
  return { ...queue, trackIds: [...queue.trackIds, trackId], currentIndex: queue.trackIds.length };
}

export function nextIndex(queue: QueueState, random: () => number = Math.random): number {
  if (!queue.trackIds.length || queue.currentIndex < 0) return -1;
  if (queue.repeat === 'one') return queue.currentIndex;
  if (queue.shuffle && queue.trackIds.length > 1) {
    const candidates = queue.trackIds.map((_, i) => i).filter((i) => i !== queue.currentIndex);
    const chosen = Math.min(candidates.length - 1, Math.floor(random() * candidates.length));
    return candidates[chosen] ?? queue.currentIndex;
  }
  if (queue.currentIndex < queue.trackIds.length - 1) return queue.currentIndex + 1;
  return queue.repeat === 'all' ? 0 : -1;
}

export function previousIndex(queue: QueueState): number {
  if (!queue.trackIds.length || queue.currentIndex < 0) return -1;
  if (queue.currentIndex > 0) return queue.currentIndex - 1;
  return queue.repeat === 'all' ? queue.trackIds.length - 1 : queue.currentIndex;
}
