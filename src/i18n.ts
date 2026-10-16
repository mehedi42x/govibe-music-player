const messages = {
  en: {
    home: 'Home',
    search: 'Search',
    library: 'Library',
    playlists: 'Playlists',
    queue: 'Queue',
    extensions: 'Extensions',
    settings: 'Settings',
  },
  bn: {
    home: 'হোম',
    search: 'খুঁজুন',
    library: 'লাইব্রেরি',
    playlists: 'প্লেলিস্ট',
    queue: 'কিউ',
    extensions: 'এক্সটেনশন',
    settings: 'সেটিংস',
  },
} as const;

export type Locale = keyof typeof messages;
export type MessageKey = keyof typeof messages.en;

export function t(locale: Locale, key: MessageKey): string {
  return messages[locale][key] ?? messages.en[key];
}
