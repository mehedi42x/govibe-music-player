// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

use std::ffi::c_void;

pub const REPEAT_OFF: i32 = 0;
pub const REPEAT_ALL: i32 = 1;
pub const REPEAT_ONE: i32 = 2;

/// Returns a valid initial index or -1 when the queue is empty.
pub fn safe_start_index(track_count: i32, requested_index: i32) -> i32 {
    if track_count <= 0 {
        -1
    } else {
        requested_index.clamp(0, track_count - 1)
    }
}

/// Chooses the next queue index. A missing successor is represented by -1.
pub fn next_index(
    track_count: i32,
    current_index: i32,
    repeat_mode: i32,
    shuffle: bool,
    seed: u64,
) -> i32 {
    if !valid_current(track_count, current_index) {
        return -1;
    }
    if repeat_mode == REPEAT_ONE {
        return current_index;
    }
    if shuffle && track_count > 1 {
        return shuffled_other_index(track_count, current_index, seed);
    }
    if current_index + 1 < track_count {
        return current_index + 1;
    }
    if repeat_mode == REPEAT_ALL {
        0
    } else {
        -1
    }
}

/// Chooses the previous queue index. A missing predecessor is represented by -1.
pub fn previous_index(
    track_count: i32,
    current_index: i32,
    repeat_mode: i32,
    shuffle: bool,
    seed: u64,
) -> i32 {
    if !valid_current(track_count, current_index) {
        return -1;
    }
    if repeat_mode == REPEAT_ONE {
        return current_index;
    }
    if shuffle && track_count > 1 {
        return shuffled_other_index(track_count, current_index, seed);
    }
    if current_index > 0 {
        current_index - 1
    } else if repeat_mode == REPEAT_ALL {
        track_count - 1
    } else {
        -1
    }
}

fn valid_current(track_count: i32, current_index: i32) -> bool {
    track_count > 0 && (0..track_count).contains(&current_index)
}

fn shuffled_other_index(track_count: i32, current_index: i32, seed: u64) -> i32 {
    // Pick from [0, track_count - 2], then skip over the current index so it
    // cannot immediately select the same track. SplitMix64 keeps this pure and
    // avoids introducing a third-party random-number dependency.
    let candidate = (splitmix64(seed) % (track_count as u64 - 1)) as i32;
    if candidate >= current_index {
        candidate + 1
    } else {
        candidate
    }
}

fn splitmix64(seed: u64) -> u64 {
    let mut value = seed.wrapping_add(0x9E37_79B9_7F4A_7C15);
    value = (value ^ (value >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
    value = (value ^ (value >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
    value ^ (value >> 31)
}

// JNI methods are deliberately primitive-only: the Rust library has no extra
// runtime dependencies and Android's Kotlin layer owns parsing, UI, storage,
// permissions, and Media3 lifecycle.
#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_com_aurora_musicplayer_core_NativePlaybackCore_safeStartIndex(
    _env: *mut c_void,
    _this: *mut c_void,
    track_count: i32,
    requested_index: i32,
) -> i32 {
    safe_start_index(track_count, requested_index)
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_com_aurora_musicplayer_core_NativePlaybackCore_nextIndex(
    _env: *mut c_void,
    _this: *mut c_void,
    track_count: i32,
    current_index: i32,
    repeat_mode: i32,
    shuffle: u8,
    seed: i64,
) -> i32 {
    next_index(
        track_count,
        current_index,
        repeat_mode,
        shuffle != 0,
        seed as u64,
    )
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_com_aurora_musicplayer_core_NativePlaybackCore_previousIndex(
    _env: *mut c_void,
    _this: *mut c_void,
    track_count: i32,
    current_index: i32,
    repeat_mode: i32,
    shuffle: u8,
    seed: i64,
) -> i32 {
    previous_index(
        track_count,
        current_index,
        repeat_mode,
        shuffle != 0,
        seed as u64,
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn safe_start_index_handles_empty_and_out_of_range_requests() {
        assert_eq!(safe_start_index(0, 0), -1);
        assert_eq!(safe_start_index(-2, 0), -1);
        assert_eq!(safe_start_index(5, -7), 0);
        assert_eq!(safe_start_index(5, 2), 2);
        assert_eq!(safe_start_index(5, 9), 4);
    }

    #[test]
    fn sequential_navigation_obeys_queue_ends() {
        assert_eq!(next_index(3, 0, REPEAT_OFF, false, 0), 1);
        assert_eq!(next_index(3, 2, REPEAT_OFF, false, 0), -1);
        assert_eq!(previous_index(3, 2, REPEAT_OFF, false, 0), 1);
        assert_eq!(previous_index(3, 0, REPEAT_OFF, false, 0), -1);
    }

    #[test]
    fn repeat_all_wraps_at_both_ends() {
        assert_eq!(next_index(3, 2, REPEAT_ALL, false, 0), 0);
        assert_eq!(previous_index(3, 0, REPEAT_ALL, false, 0), 2);
    }

    #[test]
    fn repeat_one_keeps_the_current_track() {
        assert_eq!(next_index(3, 1, REPEAT_ONE, false, 42), 1);
        assert_eq!(previous_index(3, 1, REPEAT_ONE, false, 42), 1);
    }

    #[test]
    fn shuffle_never_selects_the_current_track() {
        for count in 2..40 {
            for current in 0..count {
                let next = next_index(count, current, REPEAT_OFF, true, 0xA551 + current as u64);
                let previous =
                    previous_index(count, current, REPEAT_OFF, true, 0xB771 + current as u64);
                assert!((0..count).contains(&next));
                assert!((0..count).contains(&previous));
                assert_ne!(next, current);
                assert_ne!(previous, current);
            }
        }
    }

    #[test]
    fn a_single_item_shuffle_stays_on_that_item_only_when_repeating_one() {
        assert_eq!(next_index(1, 0, REPEAT_OFF, true, 7), -1);
        assert_eq!(previous_index(1, 0, REPEAT_ALL, true, 7), 0);
        assert_eq!(next_index(1, 0, REPEAT_ONE, true, 7), 0);
    }

    #[test]
    fn invalid_current_index_never_produces_a_queue_index() {
        assert_eq!(next_index(3, -1, REPEAT_ALL, true, 9), -1);
        assert_eq!(previous_index(3, 3, REPEAT_ALL, false, 9), -1);
    }

    #[test]
    fn shuffle_is_reproducible_for_the_same_seed() {
        assert_eq!(
            next_index(8, 2, REPEAT_OFF, true, 1234),
            next_index(8, 2, REPEAT_OFF, true, 1234)
        );
        assert_ne!(
            next_index(8, 2, REPEAT_OFF, true, 1234),
            next_index(8, 2, REPEAT_OFF, true, 1235)
        );
    }
}
