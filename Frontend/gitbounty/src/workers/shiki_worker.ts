// Owns the Shiki highlighter for the app's lifetime. This worker is created
// once by RepositoryPage and reused for every file the user opens — it is
// not recreated per-file. Recreating it per-file throws away the cached
// highlighter and re-pays the WASM/grammar init cost on every click, which
// is slower than Prism.
import { createHighlighter, bundledLanguages, type Highlighter } from 'shiki';

// Languages we expect to hit constantly are loaded eagerly at worker startup.
// Everything else is loaded on demand via loadLanguage() the first time it's
// actually necessary, so we don't pay for grammars nobody opens.
const BASE_LANGS = ['java', 'javascript', 'typescript', 'json', 'yaml', 'markdown'] as const;
const THEMES = ['github-light', 'github-dark'] as const;

let highlighterPromise: Promise<Highlighter> | null = null;

function getHighlighter(): Promise<Highlighter> {
    if (!highlighterPromise) {
        highlighterPromise = createHighlighter({
            themes: [...THEMES],
            langs: [...BASE_LANGS],
        });
    }
    return highlighterPromise;
}

const EXT_LANG_MAP: Record<string, string> = {
    java: 'java',
    js: 'javascript', mjs: 'javascript', cjs: 'javascript', jsx: 'jsx',
    ts: 'typescript', tsx: 'tsx',
    json: 'json', jsonc: 'jsonc',
    yml: 'yaml', yaml: 'yaml',
    md: 'markdown', markdown: 'markdown',
    properties: 'properties',
    ini: 'ini', cfg: 'ini', conf: 'ini',
    toml: 'toml',
    sh: 'shellscript', bash: 'shellscript', zsh: 'shellscript',
    xml: 'xml',
    gradle: 'groovy',
    kt: 'kotlin', kts: 'kotlin',
    py: 'python',
    rb: 'ruby',
    go: 'go',
    rs: 'rust',
    php: 'php',
    css: 'css', scss: 'scss',
    html: 'html', htm: 'html',
    sql: 'sql',
    dockerfile: 'dockerfile',
    makefile: 'makefile',
};

// Filenames that don't have a useful extension.
const NAME_LANG_MAP: Record<string, string> = {
    dockerfile: 'dockerfile',
    makefile: 'makefile',
    gemfile: 'ruby',
    procfile: 'yaml',
    '.gitignore': 'ini',
    '.dockerignore': 'ini',
};

function resolveLang(filename: string): string {
    const lower = filename.toLowerCase();
    if (NAME_LANG_MAP[lower]) return NAME_LANG_MAP[lower];
    // .env, .env.local, .env.production, etc. — INI-shaped key=value grammar
    // is the closest real match Shiki ships.
    if (lower.startsWith('.env')) return 'ini';
    const ext = lower.includes('.') ? lower.split('.').pop()! : '';
    return EXT_LANG_MAP[ext] ?? 'text'; // 'text' is Shiki's real no-highlight fallback
}

self.onmessage = async (event: MessageEvent) => {
    const { content, isDarkMode, filename, requestId } = event.data;
    try {
        const highlighter = await getHighlighter();
        let lang = resolveLang(filename ?? '');

        // pull in any grammar outside our base set the first time we
        // see it, instead of statically bundling every language up front.
        if (lang !== 'text' && !highlighter.getLoadedLanguages().includes(lang)) {
            if (lang in bundledLanguages) {
                await highlighter.loadLanguage(lang as keyof typeof bundledLanguages);
            } else {
                lang = 'text';
            }
        }

        const html = highlighter.codeToHtml(content, {
            lang,
            theme: isDarkMode ? 'github-dark' : 'github-light',
        });
        self.postMessage({ success: true, html, requestId });
    } catch (error: any) {
        self.postMessage({ success: false, error: error?.message ?? 'Unknown highlighting error', requestId });
    }
};