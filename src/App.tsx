import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react';
import {
  Activity,
  Album,
  AudioLines,
  Check,
  ChevronDown,
  Clock3,
  Disc3,
  Globe2,
  Heart,
  Headphones,
  Library,
  ListMusic,
  LoaderCircle,
  Menu,
  MoreHorizontal,
  Music2,
  Pause,
  Play,
  Plus,
  Radio,
  Repeat2,
  Search,
  Settings as SettingsIcon,
  ShieldCheck,
  Shuffle,
  SkipBack,
  SkipForward,
  SlidersHorizontal,
  Sparkles,
  Sun,
  Trash2,
  Upload,
  Volume2,
  VolumeX,
  X,
} from 'lucide-react';
import { parseBlob } from 'music-metadata';
import { libraryDb } from './data/library-db';
import {
  createQueue,
  enqueue,
  moveInQueue,
  nextIndex,
  previousIndex,
  removeFromQueue,
  type QueueState,
} from './core/queue';
import { createLocalLibraryProvider } from './plugins/local-library';
import {
  DEFAULT_SETTINGS,
  type AppSection,
  type AppSettings,
  type PlaybackRecord,
  type Playlist,
  type PlayerStatus,
  type Track,
} from './domain/models';
import { t } from './i18n';
import './styles.css';

const NAV = [
  { id: 'home', key: 'home', icon: Headphones },
  { id: 'search', key: 'search', icon: Search },
  { id: 'library', key: 'library', icon: Library },
  { id: 'playlists', key: 'playlists', icon: ListMusic },
  { id: 'queue', key: 'queue', icon: AudioLines },
  { id: 'extensions', key: 'extensions', icon: Sparkles },
  { id: 'settings', key: 'settings', icon: SettingsIcon },
] as const satisfies ReadonlyArray<{ id: AppSection; key: Parameters<typeof t>[1]; icon: typeof Headphones }>;

const createId = () =>
  globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`;
const timestamp = () => Date.now();
const formatTime = (seconds: number) => {
  if (!Number.isFinite(seconds) || seconds <= 0) return '0:00';
  const minutes = Math.floor(seconds / 60);
  return `${minutes}:${String(Math.floor(seconds % 60)).padStart(2, '0')}`;
};
const fallbackTitle = (name: string) =>
  name
    .replace(/\.[^.]+$/, '')
    .replace(/[._]+/g, ' ')
    .trim() || 'Untitled track';

function artworkData(picture?: { data: Uint8Array; format: string }): string | undefined {
  if (!picture?.data?.length) return undefined;
  try {
    let binary = '';
    const bytes = picture.data;
    for (let offset = 0; offset < bytes.length; offset += 0x8000) {
      binary += String.fromCharCode(...bytes.subarray(offset, Math.min(offset + 0x8000, bytes.length)));
    }
    return `data:${picture.format || 'image/jpeg'};base64,${btoa(binary)}`;
  } catch {
    return undefined;
  }
}

function Cover({ track, large = false }: { track?: Track; large?: boolean }) {
  return track?.artwork ? (
    <img
      className={`cover ${large ? 'cover-large' : ''}`}
      src={track.artwork}
      alt={`${track.album || track.title} artwork`}
    />
  ) : (
    <div
      className={`cover cover-generated ${large ? 'cover-large' : ''}`}
      aria-label="Generated album artwork"
    >
      <AudioLines size={large ? 48 : 22} strokeWidth={1.4} />
    </div>
  );
}

function TrackRow({
  track,
  index,
  active,
  playing,
  onPlay,
  onFavorite,
  onQueue,
  onDetails,
  onRemove,
  favoriteLabel,
  showPosition = true,
}: {
  track: Track;
  index: number;
  active: boolean;
  playing: boolean;
  onPlay: () => void;
  onFavorite: () => void;
  onQueue: () => void;
  onDetails: () => void;
  onRemove?: () => void;
  favoriteLabel: string;
  showPosition?: boolean;
}) {
  return (
    <div className={`track-row ${active ? 'track-row-active' : ''}`}>
      <button
        className="track-main"
        onClick={onPlay}
        aria-label={`${playing && active ? 'Pause' : 'Play'} ${track.title}`}
      >
        {showPosition && (
          <span className="track-index">
            {active && playing ? <AudioLines size={15} /> : String(index + 1).padStart(2, '0')}
          </span>
        )}
        <Cover track={track} />
        <span className="track-copy">
          <strong>{track.title}</strong>
          <span>
            {track.artist || 'Unknown artist'} <i>·</i> {track.album || 'Unknown album'}
          </span>
        </span>
        <span className="track-time">{formatTime(track.duration)}</span>
      </button>
      <div className="track-actions">
        <button
          className={`icon-button small ${track.favorite ? 'is-favorite' : ''}`}
          onClick={onFavorite}
          aria-label={track.favorite ? 'Remove from favorites' : favoriteLabel}
          title={favoriteLabel}
        >
          <Heart size={16} fill={track.favorite ? 'currentColor' : 'none'} />
        </button>
        <button
          className="icon-button small"
          onClick={onQueue}
          aria-label={`Add ${track.title} to queue`}
          title="Add to queue"
        >
          <Plus size={17} />
        </button>
        <button
          className="icon-button small detail-action"
          onClick={onDetails}
          aria-label={`Details for ${track.title}`}
          title="Track details"
        >
          <MoreHorizontal size={18} />
        </button>
        {onRemove && (
          <button
            className="icon-button small danger-action"
            onClick={onRemove}
            aria-label={`Remove ${track.title}`}
            title="Remove"
          >
            <X size={16} />
          </button>
        )}
      </div>
    </div>
  );
}

function Visualizer({ analyser, playing }: { analyser: AnalyserNode | null; playing: boolean }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    const context = canvas?.getContext('2d');
    if (!canvas || !context || !analyser || !playing) {
      if (canvas && context) context.clearRect(0, 0, canvas.width, canvas.height);
      return;
    }
    const bins = new Uint8Array(analyser.frequencyBinCount);
    let frame = 0;
    let request = 0;
    const paint = () => {
      analyser.getByteFrequencyData(bins);
      const { width, height } = canvas;
      context.clearRect(0, 0, width, height);
      const count = 42;
      const gap = 4;
      const barWidth = (width - gap * (count - 1)) / count;
      const gradient = context.createLinearGradient(0, height, width, 0);
      gradient.addColorStop(0, '#7c3aed');
      gradient.addColorStop(1, '#3dd9eb');
      context.fillStyle = gradient;
      for (let i = 0; i < count; i += 1) {
        const value = bins[Math.floor((i * bins.length) / count)] ?? 0;
        const barHeight = Math.max(3, (value / 255) * height * 0.88);
        context.globalAlpha = 0.45 + (value / 255) * 0.55;
        context.beginPath();
        context.roundRect(i * (barWidth + gap), (height - barHeight) / 2, barWidth, barHeight, 3);
        context.fill();
      }
      context.globalAlpha = 1;
      frame += 1;
      if (frame % 2 === 0) request = requestAnimationFrame(paint);
      else request = requestAnimationFrame(paint);
    };
    request = requestAnimationFrame(paint);
    return () => cancelAnimationFrame(request);
  }, [analyser, playing]);
  return (
    <div className="visualizer-wrap">
      {analyser && playing ? (
        <>
          <canvas ref={canvasRef} width={640} height={100} aria-label="Live audio frequency visualizer" />
          <span className="signal-label">
            <span className="live-dot" />
            LIVE AUDIO SIGNAL
          </span>
        </>
      ) : (
        <div className="visualizer-placeholder">
          <Activity size={18} />
          <span>Play a track to activate live audio analysis.</span>
        </div>
      )}
    </div>
  );
}

export default function App() {
  const audioRef = useRef<HTMLAudioElement>(null);
  const objectUrlRef = useRef<string | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [tracks, setTracks] = useState<Track[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [history, setHistory] = useState<PlaybackRecord[]>([]);
  const [searchResults, setSearchResults] = useState<Track[]>([]);
  const [searchResultQuery, setSearchResultQuery] = useState('');
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [queue, setQueue] = useState<QueueState>(() => createQueue());
  const [section, setSection] = useState<AppSection>('home');
  const [libraryView, setLibraryView] = useState<'all' | 'albums' | 'artists' | 'favorites' | 'history'>(
    'all',
  );
  const [query, setQuery] = useState('');
  const [currentId, setCurrentId] = useState<string | null>(null);
  const [status, setStatus] = useState<PlayerStatus>('idle');
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [importing, setImporting] = useState(false);
  const [showNowPlaying, setShowNowPlaying] = useState(false);
  const [selectedTrackId, setSelectedTrackId] = useState<string | null>(null);
  const [selectedAlbum, setSelectedAlbum] = useState<string | null>(null);
  const [selectedArtist, setSelectedArtist] = useState<string | null>(null);
  const [selectedPlaylistId, setSelectedPlaylistId] = useState<string | null>(null);
  const [analyser, setAnalyser] = useState<AnalyserNode | null>(null);
  const [mobileMenu, setMobileMenu] = useState(false);
  const [ready, setReady] = useState(false);

  const currentTrack = tracks.find((track) => track.id === currentId);
  const selectedTrack = tracks.find((track) => track.id === selectedTrackId);
  const selectedPlaylist = playlists.find((playlist) => playlist.id === selectedPlaylistId);
  const provider = useMemo(() => createLocalLibraryProvider(() => tracks), [tracks]);

  useEffect(() => {
    let cancelled = false;
    void Promise.all([
      libraryDb.getTracks(),
      libraryDb.getPlaylists(),
      libraryDb.getSettings(),
      libraryDb.getHistory(),
      libraryDb.getQueue(),
    ])
      .then(([savedTracks, savedPlaylists, savedSettings, savedHistory, savedQueue]) => {
        if (cancelled) return;
        setTracks(savedTracks.sort((a, b) => b.addedAt - a.addedAt));
        setPlaylists(savedPlaylists.sort((a, b) => b.updatedAt - a.updatedAt));
        setSettings(savedSettings ?? DEFAULT_SETTINGS);
        setHistory(savedHistory.sort((a, b) => b.playedAt - a.playedAt));
        if (savedQueue) {
          const validTrackIds = new Set(savedTracks.map((track) => track.id));
          const trackIds = savedQueue.trackIds.filter((id) => validTrackIds.has(id));
          const activeId = savedQueue.trackIds[savedQueue.currentIndex];
          const activeIndex = activeId ? trackIds.indexOf(activeId) : -1;
          const currentIndex =
            activeIndex >= 0
              ? activeIndex
              : savedQueue.currentIndex < 0
                ? -1
                : Math.min(savedQueue.currentIndex, trackIds.length - 1);
          setQueue({ ...savedQueue, trackIds, currentIndex: trackIds.length ? currentIndex : -1 });
        }
        setReady(true);
      })
      .catch((cause: unknown) => {
        setError(cause instanceof Error ? cause.message : 'Could not open your local library.');
        setReady(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(
    () => () => {
      if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
      void audioContextRef.current?.close();
      void provider.dispose();
    },
    [provider],
  );

  useEffect(() => {
    if (!ready) return;
    void libraryDb.putQueue(queue).catch(() => setError('Queue snapshot could not be saved.'));
  }, [queue, ready]);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    audio.volume = settings.volume;
  }, [settings.volume]);

  useEffect(() => {
    if (!notice) return;
    const timeout = window.setTimeout(() => setNotice(''), 3500);
    return () => window.clearTimeout(timeout);
  }, [notice]);

  function ensureAudioGraph(): AnalyserNode | null {
    const audio = audioRef.current;
    if (!audio || typeof AudioContext === 'undefined') return null;
    try {
      if (!audioContextRef.current) {
        const context = new AudioContext();
        const node = context.createAnalyser();
        node.fftSize = 256;
        node.smoothingTimeConstant = 0.82;
        const source = context.createMediaElementSource(audio);
        source.connect(node);
        node.connect(context.destination);
        audioContextRef.current = context;
        analyserRef.current = node;
        setAnalyser(node);
      }
      if (audioContextRef.current.state === 'suspended') void audioContextRef.current.resume();
      return analyserRef.current;
    } catch {
      return null;
    }
  }

  function startTrack(track: Track) {
    const audio = audioRef.current;
    if (!audio) return;
    if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
    const url = URL.createObjectURL(track.audio);
    objectUrlRef.current = url;
    setCurrentId(track.id);
    setProgress(0);
    setDuration(track.duration || 0);
    setStatus('loading');
    ensureAudioGraph();
    audio.src = url;
    audio.load();
    void audio
      .play()
      .then(() => setStatus('playing'))
      .catch(() => {
        setStatus('failed');
        setNotice('This audio file could not be played by your browser.');
      });
    void libraryDb
      .addHistory({ trackId: track.id, playedAt: timestamp() })
      .then(() => libraryDb.getHistory())
      .then((records) => setHistory(records.sort((a, b) => b.playedAt - a.playedAt)))
      .catch(() => undefined);
  }

  function playFromList(track: Track, list: Track[]) {
    const ids = list.map((item) => item.id);
    const index = ids.indexOf(track.id);
    setQueue({ ...createQueue(ids, index), shuffle: settings.shuffle, repeat: settings.repeat });
    startTrack(track);
  }

  function playQueueIndex(index: number) {
    const id = queue.trackIds[index];
    const track = tracks.find((item) => item.id === id);
    if (!track) {
      setNotice('That track is no longer in your library.');
      return;
    }
    setQueue((previous) => ({ ...previous, currentIndex: index }));
    startTrack(track);
  }

  function advance(direction: 'next' | 'previous') {
    if (direction === 'previous' && audioRef.current && audioRef.current.currentTime > 3) {
      audioRef.current.currentTime = 0;
      return;
    }
    const next = direction === 'next' ? nextIndex(queue) : previousIndex(queue);
    if (next < 0) {
      setStatus('ended');
      return;
    }
    playQueueIndex(next);
  }

  function togglePlayback() {
    const audio = audioRef.current;
    if (!audio) return;
    if (audio.paused) {
      if (!currentTrack) {
        const first = tracks[0];
        if (first) playFromList(first, tracks);
        else void openFilePicker();
        return;
      }
      ensureAudioGraph();
      void audio
        .play()
        .then(() => setStatus('playing'))
        .catch(() => setNotice('Playback could not start. Try selecting the track again.'));
    } else audio.pause();
  }

  async function importFiles(files: FileList | File[]) {
    const selected = Array.from(files).filter((file) => {
      const mimeType = file.type;
      return (
        mimeType.startsWith('audio/') || /\.(mp3|flac|wav|m4a|aac|ogg|opus|aiff|aif|wma)$/i.test(file.name)
      );
    });
    if (!selected.length) {
      setNotice('Choose audio files such as MP3, FLAC, WAV, M4A, OGG, or AAC.');
      return;
    }
    setImporting(true);
    setError('');
    const additions: Track[] = [];
    for (const file of selected) {
      let title = fallbackTitle(file.name);
      let artist = 'Unknown artist';
      let album = 'Unknown album';
      let seconds = 0;
      let artwork: string | undefined;
      try {
        const metadata = await parseBlob(file, { duration: true });
        title = metadata.common.title || title;
        artist = metadata.common.artist || metadata.common.albumartist || artist;
        album = metadata.common.album || album;
        seconds = metadata.format.duration ?? 0;
        const picture = metadata.common.picture?.[0];
        if (picture) artwork = artworkData({ data: picture.data, format: picture.format });
      } catch {
        // Tags are optional; the filename fallback still makes the file usable.
      }
      additions.push({
        id: createId(),
        title,
        artist,
        album,
        duration: seconds,
        mimeType: file.type || 'application/octet-stream',
        size: file.size,
        addedAt: timestamp(),
        favorite: false,
        artwork,
        audio: file,
      });
    }
    try {
      if (!additions.length) return;
      await libraryDb.putTracks(additions);
      setTracks((previous) => [...additions, ...previous]);
      setNotice(
        `Imported ${additions.length} ${additions.length === 1 ? 'track' : 'tracks'} to this browser.`,
      );
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Import failed. Your browser may be out of storage space.',
      );
    } finally {
      setImporting(false);
    }
  }

  function onFileChange(event: ChangeEvent<HTMLInputElement>) {
    if (event.currentTarget.files) void importFiles(event.currentTarget.files);
    event.currentTarget.value = '';
  }

  function openFilePicker() {
    fileInputRef.current?.click();
  }
  async function toggleFavorite(track: Track) {
    const updated = { ...track, favorite: !track.favorite };
    setTracks((previous) => previous.map((item) => (item.id === track.id ? updated : item)));
    await libraryDb.putTrack(updated);
  }

  function addToQueue(track: Track) {
    setQueue((previous) => enqueue(previous, [track.id]));
    setNotice(`Added “${track.title}” to the queue.`);
  }

  function stopCurrentAudio() {
    audioRef.current?.pause();
    audioRef.current?.removeAttribute('src');
    audioRef.current?.load();
    if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
    objectUrlRef.current = null;
    setCurrentId(null);
    setProgress(0);
    setDuration(0);
    setStatus('idle');
  }

  function removeQueueItem(index: number) {
    const removedId = queue.trackIds[index];
    if (removedId && removedId === currentId) stopCurrentAudio();
    setQueue((previous) => removeFromQueue(previous, index));
  }

  async function createPlaylist() {
    const name = window.prompt('Name your playlist');
    if (!name?.trim()) return;
    const now = timestamp();
    const playlist: Playlist = {
      id: createId(),
      name: name.trim(),
      trackIds: currentTrack ? [currentTrack.id] : [],
      createdAt: now,
      updatedAt: now,
    };
    await libraryDb.putPlaylist(playlist);
    setPlaylists((previous) => [playlist, ...previous]);
    setSelectedPlaylistId(playlist.id);
    setNotice('Playlist created.');
  }

  async function updatePlaylist(playlist: Playlist, trackId: string, remove = false) {
    const trackIds = remove
      ? playlist.trackIds.filter((id) => id !== trackId)
      : playlist.trackIds.includes(trackId)
        ? playlist.trackIds
        : [...playlist.trackIds, trackId];
    const updated = { ...playlist, trackIds, updatedAt: timestamp() };
    await libraryDb.putPlaylist(updated);
    setPlaylists((previous) => previous.map((item) => (item.id === playlist.id ? updated : item)));
  }

  async function deleteTrack(track: Track) {
    if (currentId === track.id) stopCurrentAudio();
    await libraryDb.deleteTrack(track.id);
    setTracks((previous) => previous.filter((item) => item.id !== track.id));
    setQueue((previous) => {
      const index = previous.trackIds.indexOf(track.id);
      return index < 0 ? previous : removeFromQueue(previous, index);
    });
    const updatedPlaylists = playlists.map((playlist) => ({
      ...playlist,
      trackIds: playlist.trackIds.filter((id) => id !== track.id),
    }));
    await Promise.all(updatedPlaylists.map((playlist) => libraryDb.putPlaylist(playlist)));
    setPlaylists(updatedPlaylists);
    setNotice('Track removed from this browser.');
  }

  async function changeSettings(patch: Partial<AppSettings>) {
    const updated = { ...settings, ...patch };
    setSettings(updated);
    await libraryDb.putSettings(updated);
  }

  async function clearHistory() {
    await libraryDb.clearHistory();
    setHistory([]);
    setNotice('Listening history cleared.');
  }

  async function clearLocalLibrary() {
    if (
      !window.confirm(
        'Remove every imported audio file and playlist from this browser? This cannot be undone.',
      )
    )
      return;
    stopCurrentAudio();
    try {
      await libraryDb.clearLibrary();
      setTracks([]);
      setPlaylists([]);
      setHistory([]);
      setQueue(createQueue());
      setSearchResults([]);
      setSelectedPlaylistId(null);
      setSelectedTrackId(null);
      setSelectedAlbum(null);
      setSelectedArtist(null);
      setNotice('Local library cleared.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'The local library could not be cleared.');
    }
  }

  const queryText = query.trim().toLocaleLowerCase();
  useEffect(() => {
    if (!queryText) return;
    const controller = new AbortController();
    void provider
      .search(queryText, controller.signal)
      .then((results) => {
        const localTracks = new Map(tracks.map((track) => [track.id, track]));
        setSearchResults(
          results
            .map((result) => localTracks.get(result.id))
            .filter((track): track is Track => Boolean(track)),
        );
        setSearchResultQuery(queryText);
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setSearchResults([]);
          setSearchResultQuery(queryText);
        }
      });
    return () => controller.abort();
  }, [provider, tracks, queryText]);
  const visibleSearchResults = searchResultQuery === queryText ? searchResults : [];
  const historyTracks = useMemo(
    () =>
      history
        .map((record) => tracks.find((track) => track.id === record.trackId))
        .filter((track): track is Track => Boolean(track)),
    [history, tracks],
  );
  const filteredLibrary = useMemo(() => {
    let source =
      libraryView === 'favorites'
        ? tracks.filter((track) => track.favorite)
        : libraryView === 'history'
          ? historyTracks
          : tracks;
    if (selectedAlbum) source = source.filter((track) => track.album === selectedAlbum);
    if (selectedArtist) source = source.filter((track) => track.artist === selectedArtist);
    if (queryText)
      source = source.filter((track) =>
        `${track.title} ${track.artist} ${track.album}`.toLowerCase().includes(queryText),
      );
    return source;
  }, [tracks, libraryView, historyTracks, selectedAlbum, selectedArtist, queryText]);
  const playlistTracks =
    selectedPlaylist?.trackIds
      .map((id) => tracks.find((track) => track.id === id))
      .filter((track): track is Track => Boolean(track)) ?? [];
  const queueTracks = queue.trackIds
    .map((id) => tracks.find((track) => track.id === id))
    .filter((track): track is Track => Boolean(track));
  const totalDuration = tracks.reduce((sum, track) => sum + track.duration, 0);

  function renderTrackList(items: Track[], canRemove = false) {
    if (!items.length)
      return (
        <EmptyState
          icon={Music2}
          title={queryText ? 'No matches found' : 'Nothing here yet'}
          body={
            queryText
              ? 'Try a different title, artist, or album.'
              : 'Import a few audio files to bring your music into GoVibe.'
          }
          action={!queryText ? openFilePicker : undefined}
          actionLabel={!queryText ? 'Import audio' : undefined}
        />
      );
    return (
      <div className="track-list">
        {items.map((track, index) => (
          <TrackRow
            key={`${track.id}-${index}`}
            track={track}
            index={index}
            active={currentId === track.id}
            playing={status === 'playing'}
            onPlay={() => (currentId === track.id ? togglePlayback() : playFromList(track, items))}
            onFavorite={() => void toggleFavorite(track)}
            onQueue={() => addToQueue(track)}
            onDetails={() => setSelectedTrackId(track.id)}
            onRemove={canRemove ? () => void deleteTrack(track) : undefined}
            favoriteLabel={track.favorite ? 'Remove favorite' : 'Add favorite'}
          />
        ))}
      </div>
    );
  }

  function pageHeading(title: string, subtitle: string, icon: typeof Headphones = Headphones) {
    const Icon = icon;
    return (
      <div className="page-heading">
        <div>
          <div className="eyebrow">
            <Icon size={15} /> GoVibe / {section.toUpperCase()}
          </div>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
      </div>
    );
  }

  function renderHome() {
    const recent = historyTracks.slice(0, 5);
    return (
      <>
        <section className="hero-card">
          <div className="hero-copy">
            <span className="eyebrow">
              <Sparkles size={15} /> YOUR SOUND, YOUR SPACE
            </span>
            <h1>
              Music, in
              <br />
              <em>its element.</em>
            </h1>
            <p>A private, beautifully simple home for the music you already own.</p>
            <button className="primary-button" onClick={() => fileInputRef.current?.click()}>
              <Upload size={17} /> Import your music
            </button>
            <span className="hero-footnote">Files stay on this device. Nothing is uploaded.</span>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="orb orb-a" />
            <div className="orb orb-b" />
            <div className="hero-disc">
              <div className="disc-grooves" />
              <div className="disc-center">
                <AudioLines size={31} />
              </div>
            </div>
            <div className="floating-note note-one">
              <Music2 size={17} />
            </div>
            <div className="floating-note note-two">
              <Activity size={16} />
            </div>
            <div className="hero-wave">
              <i />
              <i />
              <i />
              <i />
              <i />
              <i />
              <i />
              <i />
              <i />
              <i />
              <i />
            </div>
          </div>
        </section>
        <div className="stats-grid">
          <div className="stat-card">
            <span>IN YOUR LIBRARY</span>
            <strong>{tracks.length}</strong>
            <small>tracks</small>
          </div>
          <div className="stat-card">
            <span>PLAY TIME</span>
            <strong>
              {Math.floor(totalDuration / 3600)}
              <small>h</small> {Math.floor((totalDuration % 3600) / 60)}
              <small>m</small>
            </strong>
            <small>known duration</small>
          </div>
          <div className="stat-card">
            <span>PLAYLISTS</span>
            <strong>{playlists.length}</strong>
            <small>made by you</small>
          </div>
          <div className="stat-card stat-accent">
            <span>BUILT DIFFERENT</span>
            <strong>
              <ShieldCheck size={21} /> Local-first
            </strong>
            <small>your audio stays yours</small>
          </div>
        </div>
        <section className="section-block">
          <div className="section-title">
            <div>
              <span className="eyebrow">PICK UP WHERE YOU LEFT OFF</span>
              <h2>Recently played</h2>
            </div>
            <button
              className="text-button"
              onClick={() => {
                setSection('library');
                setLibraryView('history');
              }}
            >
              View history <span>→</span>
            </button>
          </div>
          {recent.length ? (
            renderTrackList(recent)
          ) : (
            <div className="inline-empty">
              <Clock3 size={19} />
              <span>Your listening history will appear here.</span>
            </div>
          )}
        </section>
        <section className="section-block">
          <div className="section-title">
            <div>
              <span className="eyebrow">READY WHEN YOU ARE</span>
              <h2>Your library</h2>
            </div>
            <button className="text-button" onClick={() => setSection('library')}>
              Open library <span>→</span>
            </button>
          </div>
          {tracks.length ? (
            renderTrackList(tracks.slice(0, 4))
          ) : (
            <div className="feature-empty">
              <div className="empty-disc">
                <AudioLines size={29} />
              </div>
              <div>
                <strong>Your listening space is ready.</strong>
                <p>Import local audio files to start building a library that belongs to you.</p>
              </div>
              <button className="secondary-button" onClick={() => fileInputRef.current?.click()}>
                <Plus size={16} /> Add tracks
              </button>
            </div>
          )}
        </section>
      </>
    );
  }

  function renderLibrary() {
    const tabs = [
      ['all', 'All tracks'],
      ['albums', 'Albums'],
      ['artists', 'Artists'],
      ['favorites', 'Favorites'],
      ['history', 'History'],
    ] as const;
    const albums = [
      ...new Set(tracks.map((track) => track.album).filter((album) => album && album !== 'Unknown album')),
    ];
    const artists = [
      ...new Set(
        tracks.map((track) => track.artist).filter((artist) => artist && artist !== 'Unknown artist'),
      ),
    ];
    return (
      <>
        {pageHeading('Your library', 'Every track you import stays in this browser.', Library)}
        <div className="library-tabs" role="tablist">
          {tabs.map(([id, label]) => (
            <button
              key={id}
              role="tab"
              aria-selected={libraryView === id}
              className={libraryView === id ? 'selected' : ''}
              onClick={() => {
                setLibraryView(id);
                setSelectedAlbum(null);
                setSelectedArtist(null);
              }}
            >
              {label}
              {id === 'favorites' && (
                <span className="tab-count">{tracks.filter((track) => track.favorite).length}</span>
              )}
            </button>
          ))}
        </div>
        {(libraryView === 'albums' || libraryView === 'artists') && !selectedAlbum && !selectedArtist ? (
          <div className="collection-grid">
            {(libraryView === 'albums' ? albums : artists).map((name) => {
              const group = tracks.filter(
                (track) => (libraryView === 'albums' ? track.album : track.artist) === name,
              );
              const target = group[0];
              return (
                <button
                  className="collection-card"
                  key={name}
                  onClick={() =>
                    libraryView === 'albums' ? setSelectedAlbum(name) : setSelectedArtist(name)
                  }
                >
                  <Cover track={target} large />
                  <span className="collection-title">{name}</span>
                  <span className="collection-meta">
                    {group.length} {group.length === 1 ? 'track' : 'tracks'}
                  </span>
                </button>
              );
            })}
            {!albums.length && libraryView === 'albums' && (
              <EmptyState
                icon={Album}
                title="No album tags yet"
                body="GoVibe reads album names from the audio tags when available."
              />
            )}
            {!artists.length && libraryView === 'artists' && (
              <EmptyState
                icon={Disc3}
                title="No artist tags yet"
                body="Artist collections appear when imported audio contains artist metadata."
              />
            )}
          </div>
        ) : (
          <>
            {(selectedAlbum || selectedArtist) && (
              <button
                className="back-link"
                onClick={() => {
                  setSelectedAlbum(null);
                  setSelectedArtist(null);
                }}
              >
                ← Back to {libraryView}
              </button>
            )}
            <div className="list-toolbar">
              <span>
                {filteredLibrary.length} {filteredLibrary.length === 1 ? 'track' : 'tracks'}
              </span>
              <div>
                <button className="secondary-button compact" onClick={() => fileInputRef.current?.click()}>
                  <Plus size={15} /> Import
                </button>
              </div>
            </div>
            {renderTrackList(filteredLibrary, libraryView === 'all')}
          </>
        )}
      </>
    );
  }

  function renderSearch() {
    return (
      <>
        {pageHeading('Search your music', 'Find a track, artist, or album in your local collection.', Search)}
        <label className="large-search">
          <Search size={19} />
          <input
            autoFocus
            placeholder="Search titles, artists, albums…"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <kbd>LOCAL</kbd>
          {query && (
            <button className="icon-button small" onClick={() => setQuery('')} aria-label="Clear search">
              <X size={16} />
            </button>
          )}
        </label>
        {!queryText ? (
          <div className="search-hint">
            <div className="hint-icon">
              <Globe2 size={22} />
            </div>
            <div>
              <strong>Search stays on this device.</strong>
              <p>
                Only metadata from files you imported is indexed. Online catalogs are not connected in this
                release.
              </p>
            </div>
          </div>
        ) : (
          <>
            <div className="list-toolbar">
              <span>
                {visibleSearchResults.length} {visibleSearchResults.length === 1 ? 'match' : 'matches'}
              </span>
              <span className="source-chip">
                <span className="live-dot" /> Local Library
              </span>
            </div>
            {renderTrackList(visibleSearchResults)}
          </>
        )}
      </>
    );
  }

  /* eslint-disable react-hooks/refs -- these nested render helpers wire refs only to user-activated callbacks. */
  function renderPlaylists() {
    return (
      <>
        {pageHeading('Your playlists', 'Make a soundtrack for every kind of day.', ListMusic)}
        {!selectedPlaylist ? (
          <>
            <div className="list-toolbar">
              <span>
                {playlists.length} {playlists.length === 1 ? 'playlist' : 'playlists'}
              </span>
              <button className="primary-button compact" onClick={() => void createPlaylist()}>
                <Plus size={16} /> New playlist
              </button>
            </div>
            {playlists.length ? (
              <div className="playlist-grid">
                {playlists.map((playlist) => {
                  const firstTrack = tracks.find((track) => playlist.trackIds.includes(track.id));
                  return (
                    <button
                      key={playlist.id}
                      className="playlist-card"
                      onClick={() => setSelectedPlaylistId(playlist.id)}
                    >
                      <div className="playlist-art">
                        <Cover track={firstTrack} large />
                        <span className="playlist-overlay">
                          <Play size={22} fill="currentColor" />
                        </span>
                      </div>
                      <strong>{playlist.name}</strong>
                      <span>{playlist.trackIds.length} tracks</span>
                    </button>
                  );
                })}
              </div>
            ) : (
              <EmptyState
                icon={ListMusic}
                title="No playlists yet"
                body="Create one, then add tracks from your library or from the player."
                action={() => void createPlaylist()}
                actionLabel="Create a playlist"
              />
            )}
          </>
        ) : (
          <>
            <button className="back-link" onClick={() => setSelectedPlaylistId(null)}>
              ← All playlists
            </button>
            <div className="playlist-detail-head">
              <div className="playlist-cover-big">
                <Cover track={playlistTracks[0]} large />
              </div>
              <div>
                <span className="eyebrow">PLAYLIST</span>
                <h2>{selectedPlaylist.name}</h2>
                <p>
                  {playlistTracks.length} tracks · Created{' '}
                  {new Date(selectedPlaylist.createdAt).toLocaleDateString()}
                </p>
                <div className="button-row">
                  <button
                    className="primary-button compact"
                    disabled={!playlistTracks.length}
                    onClick={() => playlistTracks[0] && playFromList(playlistTracks[0], playlistTracks)}
                  >
                    <Play size={16} fill="currentColor" /> Play
                  </button>
                  <button className="secondary-button compact" onClick={() => void createPlaylist()}>
                    <Plus size={15} /> New
                  </button>
                </div>
              </div>
            </div>
            {playlistTracks.length ? (
              <div className="track-list">
                {playlistTracks.map((track, index) => (
                  <TrackRow
                    key={track.id}
                    track={track}
                    index={index}
                    active={currentId === track.id}
                    playing={status === 'playing'}
                    onPlay={() =>
                      currentId === track.id ? togglePlayback() : playFromList(track, playlistTracks)
                    }
                    onFavorite={() => void toggleFavorite(track)}
                    onQueue={() => addToQueue(track)}
                    onDetails={() => setSelectedTrackId(track.id)}
                    onRemove={() => void updatePlaylist(selectedPlaylist, track.id, true)}
                    favoriteLabel="Toggle favorite"
                  />
                ))}
              </div>
            ) : (
              <EmptyState
                icon={Music2}
                title="This playlist is empty"
                body="Add a track from its details or create another playlist while a track is playing."
              />
            )}
          </>
        )}
      </>
    );
  }

  function renderQueue() {
    return (
      <>
        {pageHeading('Up next', 'A queue for this listening session.', AudioLines)}
        <div className="queue-summary">
          <div>
            <strong>{queueTracks.length}</strong>
            <span> tracks in queue</span>
          </div>
          <div className="queue-toggles">
            <button
              className={`pill-toggle ${queue.shuffle ? 'active' : ''}`}
              onClick={() => {
                const shuffle = !queue.shuffle;
                setQueue((previous) => ({ ...previous, shuffle }));
                void changeSettings({ shuffle });
              }}
            >
              <Shuffle size={15} /> Shuffle {queue.shuffle ? 'on' : 'off'}
            </button>
            <button
              className={`pill-toggle ${queue.repeat !== 'off' ? 'active' : ''}`}
              onClick={() => {
                const repeat = queue.repeat === 'off' ? 'all' : queue.repeat === 'all' ? 'one' : 'off';
                setQueue((previous) => ({ ...previous, repeat }));
                void changeSettings({ repeat });
              }}
            >
              <Repeat2 size={15} /> Repeat{' '}
              {queue.repeat === 'one' ? 'one' : queue.repeat === 'all' ? 'all' : 'off'}
            </button>
          </div>
        </div>
        {queueTracks.length ? (
          <div className="track-list">
            {queueTracks.map((track, index) => (
              <div className="queue-row-wrap" key={`${track.id}-${index}`}>
                <TrackRow
                  track={track}
                  index={index}
                  active={queue.currentIndex === index}
                  playing={status === 'playing'}
                  onPlay={() => (currentId === track.id ? togglePlayback() : playQueueIndex(index))}
                  onFavorite={() => void toggleFavorite(track)}
                  onQueue={() => addToQueue(track)}
                  onDetails={() => setSelectedTrackId(track.id)}
                  onRemove={() => removeQueueItem(index)}
                  favoriteLabel="Toggle favorite"
                />
                <div className="queue-order">
                  <button
                    title="Move up"
                    aria-label="Move up"
                    disabled={index === 0}
                    onClick={() => setQueue((previous) => moveInQueue(previous, index, index - 1))}
                  >
                    ↑
                  </button>
                  <button
                    title="Move down"
                    aria-label="Move down"
                    disabled={index === queueTracks.length - 1}
                    onClick={() => setQueue((previous) => moveInQueue(previous, index, index + 1))}
                  >
                    ↓
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={AudioLines}
            title="Your queue is quiet"
            body="Play a track from your library to start a queue."
            action={tracks.length ? () => playFromList(tracks[0]!, tracks) : openFilePicker}
            actionLabel={tracks.length ? 'Play first track' : 'Import music'}
          />
        )}
      </>
    );
  }

  /* eslint-enable react-hooks/refs */
  function renderExtensions() {
    const providers = [
      {
        name: 'Spotify',
        kind: 'Metadata / remote playback',
        status: 'Planned',
        note: 'Requires approved Spotify API access and an authorized playback client. Metadata access alone does not provide audio.',
      },
      {
        name: 'Last.fm',
        kind: 'Discovery / scrobbling',
        status: 'Planned',
        note: 'A discovery and listening-history service, not a source of playable audio in this release.',
      },
    ];
    return (
      <>
        {pageHeading(
          'Extensions',
          'The browser build has no bundled community extensions or remote provider runtime.',
          Sparkles,
        )}
        <div className="security-banner">
          <ShieldCheck size={21} />
          <div>
            <strong>Community code is untrusted by default.</strong>
            <p>
              External code packages are not loaded or executed by this web app. The native Android app
              accepts declarative JSON home profiles; the browser build keeps its local-library tools in the
              app itself.
            </p>
          </div>
        </div>
        <section className="provider-section">
          <div className="section-title">
            <div>
              <span className="eyebrow">INTEGRATION STATUS</span>
              <h2>Integrations</h2>
            </div>
          </div>
          <div className="provider-list">
            {providers.map((item) => (
              <div className="provider-card provider-planned" key={item.name}>
                <div className="provider-icon">
                  <Radio size={21} />
                </div>
                <div className="provider-copy">
                  <strong>{item.name}</strong>
                  <span>{item.kind}</span>
                  <small>{item.note}</small>
                </div>
                <span className="status-tag">{item.status}</span>
              </div>
            ))}
          </div>
        </section>
        <div className="sdk-note">
          <Sparkles size={18} />
          <span>
            Plugin SDK <strong>v1.0.0</strong> · manifest validation is active · third-party runtime is not
            yet available
          </span>
        </div>
      </>
    );
  }

  function renderSettings() {
    return (
      <>
        {pageHeading('Settings', 'A few thoughtful controls, with privacy as the default.', SettingsIcon)}
        <section className="settings-group">
          <div className="settings-title">
            <SlidersHorizontal size={18} />
            <div>
              <strong>Playback & sound</strong>
              <span>Controls for your current listening session.</span>
            </div>
          </div>
          <label className="setting-row">
            <span>
              <strong>Preferred quality</strong>
              <small>Applies only when a provider offers selectable formats.</small>
            </span>
            <select
              value={settings.quality}
              onChange={(event) =>
                void changeSettings({ quality: event.target.value as AppSettings['quality'] })
              }
            >
              <option value="auto">Auto</option>
              <option value="low">Low</option>
              <option value="medium">Medium</option>
              <option value="high">High</option>
              <option value="lossless">Lossless (only when available)</option>
              <option value="maximum">Maximum available</option>
            </select>
          </label>
          <label className="setting-row">
            <span>
              <strong>Volume</strong>
              <small>Current device playback level.</small>
            </span>
            <input
              className="setting-range"
              type="range"
              min="0"
              max="1"
              step="0.01"
              value={settings.volume}
              onChange={(event) => void changeSettings({ volume: Number(event.target.value) })}
            />
          </label>
          <label className="setting-row">
            <span>
              <strong>Default repeat</strong>
              <small>Used when a new queue starts.</small>
            </span>
            <select
              value={settings.repeat}
              onChange={(event) => {
                const repeat = event.target.value as AppSettings['repeat'];
                setQueue((previous) => ({ ...previous, repeat }));
                void changeSettings({ repeat });
              }}
            >
              <option value="off">Off</option>
              <option value="all">Repeat queue</option>
              <option value="one">Repeat track</option>
            </select>
          </label>
        </section>
        <section className="settings-group">
          <div className="settings-title">
            <Sun size={18} />
            <div>
              <strong>Appearance & language</strong>
              <span>Comfortable on a bright screen or a dark room.</span>
            </div>
          </div>
          <label className="setting-row">
            <span>
              <strong>Theme</strong>
              <small>Choose a display appearance.</small>
            </span>
            <select
              value={settings.theme}
              onChange={(event) => void changeSettings({ theme: event.target.value as AppSettings['theme'] })}
            >
              <option value="dark">Dark</option>
              <option value="light">Light</option>
              <option value="system">System (dark in this preview)</option>
            </select>
          </label>
          <label className="setting-row">
            <span>
              <strong>Language</strong>
              <small>Navigation and core labels are available in English and Bangla.</small>
            </span>
            <select
              value={settings.locale}
              onChange={(event) =>
                void changeSettings({ locale: event.target.value as AppSettings['locale'] })
              }
            >
              <option value="en">English</option>
              <option value="bn">বাংলা</option>
            </select>
          </label>
        </section>
        <section className="settings-group">
          <div className="settings-title">
            <ShieldCheck size={18} />
            <div>
              <strong>Privacy & local data</strong>
              <span>No analytics or accounts are enabled.</span>
            </div>
          </div>
          <div className="privacy-line">
            <div>
              <strong>Telemetry</strong>
              <small>Off. GoVibe does not send listening data anywhere.</small>
            </div>
            <span className="state-ready">
              <Check size={14} /> Off
            </span>
          </div>
          <div className="privacy-line">
            <div>
              <strong>Clear listening history</strong>
              <small>Removes timestamps only. Your imported audio stays put.</small>
            </div>
            <button className="secondary-button compact" onClick={() => void clearHistory()}>
              <Trash2 size={14} /> Clear history
            </button>
          </div>
          <div className="privacy-line">
            <div>
              <strong>Remove all imported music</strong>
              <small>Deletes audio copies stored by this browser and its playlists.</small>
            </div>
            <button className="danger-button compact" onClick={() => void clearLocalLibrary()}>
              <Trash2 size={14} /> Clear library
            </button>
          </div>
        </section>
        <p className="settings-footnote">
          Local playback is decoded by your browser. Sample rate, bit depth, and bit-perfect output are not
          measured by this release; no lossless playback claim is made.
        </p>
      </>
    );
  }

  const headingBySection: Record<AppSection, string> = {
    home: t(settings.locale, 'home'),
    search: t(settings.locale, 'search'),
    library: t(settings.locale, 'library'),
    playlists: t(settings.locale, 'playlists'),
    queue: t(settings.locale, 'queue'),
    extensions: t(settings.locale, 'extensions'),
    settings: t(settings.locale, 'settings'),
  };

  return (
    <div className={`app-shell ${settings.theme === 'light' ? 'theme-light' : ''}`}>
      <audio
        ref={audioRef}
        preload="metadata"
        onPlay={() => setStatus('playing')}
        onPause={() => {
          if (currentId && audioRef.current && !audioRef.current.ended) setStatus('paused');
        }}
        onWaiting={() => setStatus('buffering')}
        onCanPlay={() => {
          if (status === 'loading') setStatus('ready');
        }}
        onTimeUpdate={() => {
          if (audioRef.current) setProgress(audioRef.current.currentTime);
        }}
        onDurationChange={() => {
          if (audioRef.current && Number.isFinite(audioRef.current.duration))
            setDuration(audioRef.current.duration);
        }}
        onEnded={() => advance('next')}
        onError={() => {
          if (currentId) {
            setStatus('failed');
            setNotice('This file format may not be supported by your browser.');
          }
        }}
      />
      <input
        ref={fileInputRef}
        type="file"
        accept="audio/*,.mp3,.flac,.wav,.m4a,.aac,.ogg,.opus,.aiff,.aif,.wma"
        multiple
        hidden
        onChange={onFileChange}
      />
      <aside className={`sidebar ${mobileMenu ? 'sidebar-open' : ''}`}>
        <button
          className="brand"
          onClick={() => {
            setSection('home');
            setMobileMenu(false);
          }}
          aria-label="GoVibe home"
        >
          <span className="brand-mark">
            <AudioLines size={20} />
          </span>
          <span>
            GoVibe<small>LOCAL MUSIC PLAYER</small>
          </span>
        </button>
        <div className="nav-caption">YOUR SPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          {NAV.map(({ id, key, icon: Icon }) => (
            <button
              key={id}
              className={`nav-item ${section === id ? 'active' : ''}`}
              onClick={() => {
                setSection(id);
                setMobileMenu(false);
              }}
            >
              <Icon size={18} />
              <span>{t(settings.locale, key)}</span>
              {id === 'queue' && queueTracks.length > 0 && <i className="nav-count">{queueTracks.length}</i>}
              {section === id && <i className="nav-indicator" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-line" />
          <div className="privacy-pill">
            <span className="privacy-dot" />
            <div>
              <strong>Your library is private</strong>
              <small>Stored on this device</small>
            </div>
          </div>
          <button className="import-sidebar" onClick={() => fileInputRef.current?.click()}>
            <Upload size={16} /> Import audio
          </button>
          <span className="version-label">
            GoVibe <i>·</i> v0.1.0
          </span>
        </div>
      </aside>
      {mobileMenu && (
        <button className="mobile-scrim" onClick={() => setMobileMenu(false)} aria-label="Close navigation" />
      )}
      <main className="main-area">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="mobile-menu-button icon-button"
              onClick={() => setMobileMenu(true)}
              aria-label="Open navigation"
            >
              <Menu size={19} />
            </button>
            <span className="breadcrumb">
              GoVibe <span>/</span> {headingBySection[section]}
            </span>
          </div>
          <div className="topbar-right">
            <label className="top-search">
              <Search size={16} />
              <input
                aria-label="Search local library"
                placeholder="Search your library…"
                value={query}
                onChange={(event) => {
                  setQuery(event.target.value);
                  if (event.target.value) setSection('search');
                }}
              />
              <kbd>⌘ K</kbd>
            </label>
            <button className="avatar-button" title="Local profile">
              <span>M</span>
            </button>
          </div>
        </header>
        <div className="page-scroll">
          <div className="page-content">
            {!ready && (
              <div className="loading-state">
                <LoaderCircle className="spin" size={22} /> Opening your local library…
              </div>
            )}
            {error && (
              <div className="error-banner">
                <X size={18} />
                <span>{error}</span>
                <button onClick={() => setError('')} aria-label="Dismiss error">
                  <X size={15} />
                </button>
              </div>
            )}
            {ready && (
              <>
                {section === 'home' && renderHome()}
                {section === 'search' && renderSearch()}
                {section === 'library' && renderLibrary()}
                {section === 'playlists' && renderPlaylists()}
                {section === 'queue' && renderQueue()}
                {section === 'extensions' && renderExtensions()}
                {section === 'settings' && renderSettings()}
              </>
            )}
          </div>
        </div>
        <footer className={`mini-player ${currentTrack ? '' : 'mini-player-empty'}`}>
          <div
            className="mini-track"
            onClick={() => currentTrack && setShowNowPlaying(true)}
            role={currentTrack ? 'button' : undefined}
            tabIndex={currentTrack ? 0 : undefined}
            onKeyDown={(event) => {
              if (currentTrack && (event.key === 'Enter' || event.key === ' ')) setShowNowPlaying(true);
            }}
          >
            {currentTrack ? (
              <>
                <Cover track={currentTrack} />
                <span className="mini-copy">
                  <strong>{currentTrack.title}</strong>
                  <small>{currentTrack.artist}</small>
                </span>
                <button
                  className={`icon-button small mini-fav ${currentTrack.favorite ? 'is-favorite' : ''}`}
                  onClick={(event) => {
                    event.stopPropagation();
                    void toggleFavorite(currentTrack);
                  }}
                  aria-label="Toggle favorite"
                >
                  <Heart size={16} fill={currentTrack.favorite ? 'currentColor' : 'none'} />
                </button>
              </>
            ) : (
              <>
                <span className="mini-empty-cover">
                  <Music2 size={17} />
                </span>
                <span className="mini-copy">
                  <strong>Nothing playing</strong>
                  <small>{tracks.length ? 'Choose a track to begin' : 'Import your music to begin'}</small>
                </span>
              </>
            )}
          </div>
          <div className="player-controls">
            <div className="transport">
              <button
                className={`control-button ${queue.shuffle ? 'control-active' : ''}`}
                onClick={() => {
                  const shuffle = !queue.shuffle;
                  setQueue((previous) => ({ ...previous, shuffle }));
                  void changeSettings({ shuffle });
                }}
                aria-label="Toggle shuffle"
              >
                <Shuffle size={16} />
              </button>
              <button
                className="control-button"
                onClick={() => advance('previous')}
                aria-label="Previous track"
              >
                <SkipBack size={18} fill="currentColor" />
              </button>
              <button
                className="play-button"
                onClick={togglePlayback}
                aria-label={status === 'playing' ? 'Pause' : 'Play'}
              >
                {status === 'playing' ? (
                  <Pause size={18} fill="currentColor" />
                ) : status === 'loading' || importing ? (
                  <LoaderCircle className="spin" size={18} />
                ) : (
                  <Play size={18} fill="currentColor" />
                )}
              </button>
              <button className="control-button" onClick={() => advance('next')} aria-label="Next track">
                <SkipForward size={18} fill="currentColor" />
              </button>
              <button
                className={`control-button ${queue.repeat !== 'off' ? 'control-active' : ''}`}
                onClick={() => {
                  const repeat = queue.repeat === 'off' ? 'all' : queue.repeat === 'all' ? 'one' : 'off';
                  setQueue((previous) => ({ ...previous, repeat }));
                  void changeSettings({ repeat });
                }}
                aria-label={`Repeat ${queue.repeat}`}
              >
                <Repeat2 size={16} />
                {queue.repeat === 'one' && <sup>1</sup>}
              </button>
            </div>
            <div className="progress-row">
              <span>{formatTime(progress)}</span>
              <input
                aria-label="Track progress"
                type="range"
                min="0"
                max={Math.max(duration, 1)}
                step="0.1"
                value={Math.min(progress, duration || 1)}
                onChange={(event) => {
                  const value = Number(event.target.value);
                  if (audioRef.current) audioRef.current.currentTime = value;
                  setProgress(value);
                }}
              />
              <span>{formatTime(duration)}</span>
            </div>
          </div>
          <div className="player-right">
            <button
              className="icon-button small now-playing-open"
              onClick={() => currentTrack && setShowNowPlaying(true)}
              disabled={!currentTrack}
              aria-label="Open now playing"
            >
              <AudioLines size={17} />
            </button>
            <div className="volume-control">
              <button
                className="icon-button small"
                onClick={() => void changeSettings({ volume: settings.volume > 0 ? 0 : 0.78 })}
                aria-label="Toggle mute"
              >
                {settings.volume === 0 ? <VolumeX size={17} /> : <Volume2 size={17} />}
              </button>
              <input
                aria-label="Volume"
                type="range"
                min="0"
                max="1"
                step="0.01"
                value={settings.volume}
                onChange={(event) => void changeSettings({ volume: Number(event.target.value) })}
              />
            </div>
            <span className="format-pill">LOCAL</span>
          </div>
        </footer>
      </main>
      {notice && (
        <div className="toast" role="status">
          <Check size={16} /> {notice}
        </div>
      )}
      {importing && (
        <div className="import-progress">
          <LoaderCircle className="spin" size={16} /> Reading local audio tags…
        </div>
      )}
      {selectedTrack && (
        <div
          className="modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setSelectedTrackId(null);
          }}
        >
          <section
            className="track-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="track-modal-title"
          >
            <button
              className="modal-close icon-button"
              onClick={() => setSelectedTrackId(null)}
              aria-label="Close details"
            >
              <X size={18} />
            </button>
            <Cover track={selectedTrack} large />
            <span className="eyebrow">TRACK DETAILS</span>
            <h2 id="track-modal-title">{selectedTrack.title}</h2>
            <p>
              {selectedTrack.artist} · {selectedTrack.album}
            </p>
            <div className="metadata-grid">
              <div>
                <span>FORMAT</span>
                <strong>{selectedTrack.mimeType.replace('audio/', '').toUpperCase() || 'Unknown'}</strong>
              </div>
              <div>
                <span>DURATION</span>
                <strong>{formatTime(selectedTrack.duration)}</strong>
              </div>
              <div>
                <span>FILE SIZE</span>
                <strong>{(selectedTrack.size / 1024 / 1024).toFixed(1)} MB</strong>
              </div>
              <div>
                <span>QUALITY</span>
                <strong>Not measured</strong>
              </div>
            </div>
            <div className="detail-note">
              GoVibe does not infer bitrate or bit depth from file extension. Playback is handled by this
              browser.
            </div>
            <div className="button-row">
              <button
                className="primary-button"
                onClick={() => {
                  playFromList(selectedTrack, tracks);
                  setSelectedTrackId(null);
                }}
              >
                <Play size={16} fill="currentColor" /> Play track
              </button>
              <button className="secondary-button" onClick={() => addToQueue(selectedTrack)}>
                <Plus size={16} /> Add to queue
              </button>
              <button
                className="secondary-button"
                onClick={() => {
                  const playlist = playlists[0];
                  if (playlist)
                    void updatePlaylist(playlist, selectedTrack.id).then(() =>
                      setNotice(`Added to ${playlist.name}.`),
                    );
                  else void createPlaylist();
                }}
              >
                <ListMusic size={16} /> Add to playlist
              </button>
            </div>
          </section>
        </div>
      )}
      {showNowPlaying && currentTrack && (
        <div className="now-playing-overlay">
          <button
            className="now-playing-close icon-button"
            onClick={() => setShowNowPlaying(false)}
            aria-label="Close now playing"
          >
            <ChevronDown size={21} />
          </button>
          <div className="now-playing-inner">
            <div className="now-art">
              <Cover track={currentTrack} large />
            </div>
            <span className="eyebrow">
              <AudioLines size={15} /> NOW PLAYING · LOCAL FILE
            </span>
            <h1>{currentTrack.title}</h1>
            <p className="now-artist">
              {currentTrack.artist} <i>·</i> {currentTrack.album}
            </p>
            <Visualizer analyser={analyser} playing={status === 'playing'} />
            <div className="now-progress">
              <span>{formatTime(progress)}</span>
              <input
                type="range"
                aria-label="Now playing progress"
                min="0"
                max={Math.max(duration, 1)}
                step="0.1"
                value={Math.min(progress, duration || 1)}
                onChange={(event) => {
                  if (audioRef.current) audioRef.current.currentTime = Number(event.target.value);
                }}
              />
              <span>{formatTime(duration)}</span>
            </div>
            <div className="now-controls">
              <button
                className={`control-button ${queue.shuffle ? 'control-active' : ''}`}
                onClick={() => {
                  const shuffle = !queue.shuffle;
                  setQueue((previous) => ({ ...previous, shuffle }));
                  void changeSettings({ shuffle });
                }}
                aria-label="Toggle shuffle"
              >
                <Shuffle size={20} />
              </button>
              <button className="control-button" onClick={() => advance('previous')} aria-label="Previous">
                <SkipBack size={22} fill="currentColor" />
              </button>
              <button
                className="play-button play-button-large"
                onClick={togglePlayback}
                aria-label={status === 'playing' ? 'Pause' : 'Play'}
              >
                {status === 'playing' ? (
                  <Pause size={25} fill="currentColor" />
                ) : (
                  <Play size={25} fill="currentColor" />
                )}
              </button>
              <button className="control-button" onClick={() => advance('next')} aria-label="Next">
                <SkipForward size={22} fill="currentColor" />
              </button>
              <button
                className={`control-button ${queue.repeat !== 'off' ? 'control-active' : ''}`}
                onClick={() => {
                  const repeat = queue.repeat === 'off' ? 'all' : queue.repeat === 'all' ? 'one' : 'off';
                  setQueue((previous) => ({ ...previous, repeat }));
                  void changeSettings({ repeat });
                }}
                aria-label="Toggle repeat"
              >
                <Repeat2 size={20} />
              </button>
            </div>
            <div className="now-bottom">
              <span>
                <ShieldCheck size={15} /> Local audio · properties not measured
              </span>
              <button
                className={`icon-button ${currentTrack.favorite ? 'is-favorite' : ''}`}
                onClick={() => void toggleFavorite(currentTrack)}
                aria-label="Toggle favorite"
              >
                <Heart size={18} fill={currentTrack.favorite ? 'currentColor' : 'none'} />
              </button>
              <button
                className="secondary-button compact"
                onClick={() => {
                  setShowNowPlaying(false);
                  setSection('queue');
                }}
              >
                <AudioLines size={15} /> Queue ({queueTracks.length})
              </button>
            </div>
            <div className="lyrics-note">Lyrics aren't available for local files in this release.</div>
          </div>
        </div>
      )}
      <nav className="mobile-nav" aria-label="Mobile navigation">
        {NAV.filter((item) => ['home', 'search', 'library', 'playlists', 'settings'].includes(item.id)).map(
          ({ id, key, icon: Icon }) => (
            <button key={id} className={section === id ? 'active' : ''} onClick={() => setSection(id)}>
              <Icon size={18} />
              <span>{t(settings.locale, key)}</span>
            </button>
          ),
        )}
      </nav>
      <span className="sr-only" aria-live="polite">
        {status === 'playing'
          ? `Playing ${currentTrack?.title ?? ''}`
          : status === 'paused'
            ? 'Playback paused'
            : ''}
      </span>
    </div>
  );
}

function EmptyState({
  icon: Icon,
  title,
  body,
  action,
  actionLabel,
}: {
  icon: typeof Headphones;
  title: string;
  body: string;
  action?: () => void;
  actionLabel?: string;
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">
        <Icon size={23} />
      </div>
      <strong>{title}</strong>
      <p>{body}</p>
      {action && actionLabel && (
        <button className="secondary-button" onClick={action}>
          <Plus size={16} /> {actionLabel}
        </button>
      )}
    </div>
  );
}
