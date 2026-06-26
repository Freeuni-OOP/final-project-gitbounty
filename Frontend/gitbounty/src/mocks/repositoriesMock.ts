export type FileNode = {
  name: string;
  type: 'file' | 'dir';
  content?: string;
  children?: FileNode[];
  lastCommit: string;
  commitTime: string;
};

export type MockRepo = {
  owner: string;
  name: string;
  description: string;
  language: string;
  languageColor: string;
  stars: number;
  forks: number;
  lastUpdated: string;
};

export const mockRepositories: MockRepo[] = [
  {
    owner: 'torvalds',
    name: 'linux',
    description: 'Linux kernel source tree',
    language: 'C',
    languageColor: '#555555',
    stars: 182341,
    forks: 52918,
    lastUpdated: '2 hours ago',
  },
  {
    owner: 'microsoft',
    name: 'vscode',
    description: 'Visual Studio Code — open-source code editor',
    language: 'TypeScript',
    languageColor: '#3178c6',
    stars: 163420,
    forks: 29110,
    lastUpdated: '1 hour ago',
  },
  {
    owner: 'facebook',
    name: 'react',
    description: 'The library for web and native user interfaces',
    language: 'JavaScript',
    languageColor: '#f1e05a',
    stars: 228000,
    forks: 46500,
    lastUpdated: '3 hours ago',
  },
  {
    owner: 'rust-lang',
    name: 'rust',
    description: 'Empowering everyone to build reliable and efficient software',
    language: 'Rust',
    languageColor: '#dea584',
    stars: 98700,
    forks: 12800,
    lastUpdated: '5 hours ago',
  },
  {
    owner: 'golang',
    name: 'go',
    description: 'The Go programming language',
    language: 'Go',
    languageColor: '#00ADD8',
    stars: 124500,
    forks: 17800,
    lastUpdated: '4 hours ago',
  },
];

const defaultTree: FileNode[] = [
  {
    name: 'src',
    type: 'dir',
    lastCommit: 'Initial commit',
    commitTime: '3 days ago',
    children: [
      {
        name: 'components',
        type: 'dir',
        lastCommit: 'Add base components',
        commitTime: '2 days ago',
        children: [
          {
            name: 'Button.tsx',
            type: 'file',
            lastCommit: 'Style button variants',
            commitTime: '1 day ago',
            content:
              'import React from "react";\n\ntype Props = { children: React.ReactNode; onClick?: () => void };\n\nexport const Button = ({ children, onClick }: Props) => (\n  <button className="btn" onClick={onClick}>\n    {children}\n  </button>\n);\n',
          },
          {
            name: 'Input.tsx',
            type: 'file',
            lastCommit: 'Add Input component',
            commitTime: '1 day ago',
            content:
              'import React from "react";\n\ntype Props = { value: string; onChange: (v: string) => void };\n\nexport const Input = ({ value, onChange }: Props) => (\n  <input\n    value={value}\n    onChange={(e) => onChange(e.target.value)}\n    className="input"\n  />\n);\n',
          },
        ],
      },
      {
        name: 'utils',
        type: 'dir',
        lastCommit: 'Add utility helpers',
        commitTime: '4 days ago',
        children: [
          {
            name: 'format.ts',
            type: 'file',
            lastCommit: 'Add format utilities',
            commitTime: '4 days ago',
            content:
              'export const formatDate = (d: Date): string =>\n  d.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });\n\nexport const formatNumber = (n: number): string =>\n  n >= 1000 ? `${(n / 1000).toFixed(1)}k` : String(n);\n',
          },
          {
            name: 'validate.ts',
            type: 'file',
            lastCommit: 'Add validation helpers',
            commitTime: '3 days ago',
            content:
              'export const isEmail = (s: string): boolean =>\n  /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/.test(s);\n\nexport const isRequired = (s: string): boolean => s.trim().length > 0;\n\nexport const minLength = (s: string, n: number): boolean => s.length >= n;\n',
          },
        ],
      },
      {
        name: 'index.ts',
        type: 'file',
        lastCommit: 'Export main entry',
        commitTime: '3 days ago',
        content:
          'export * from "./components/Button";\nexport * from "./components/Input";\nexport * from "./utils/format";\nexport * from "./utils/validate";\n',
      },
    ],
  },
  {
    name: 'tests',
    type: 'dir',
    lastCommit: 'Add test suite',
    commitTime: '2 days ago',
    children: [
      {
        name: 'Button.test.tsx',
        type: 'file',
        lastCommit: 'Add button tests',
        commitTime: '1 day ago',
        content:
          'import { render, fireEvent } from "@testing-library/react";\nimport { Button } from "../src/components/Button";\n\ndescribe("Button", () => {\n  it("renders children", () => {\n    const { getByText } = render(<Button>Click me</Button>);\n    expect(getByText("Click me")).toBeTruthy();\n  });\n\n  it("calls onClick", () => {\n    const fn = jest.fn();\n    const { getByText } = render(<Button onClick={fn}>Go</Button>);\n    fireEvent.click(getByText("Go"));\n    expect(fn).toHaveBeenCalledTimes(1);\n  });\n});\n',
      },
    ],
  },
  {
    name: '.gitignore',
    type: 'file',
    lastCommit: 'Add gitignore',
    commitTime: '5 days ago',
    content: 'node_modules/\ndist/\n.env\n*.log\n.DS_Store\n',
  },
  {
    name: 'package.json',
    type: 'file',
    lastCommit: 'Init project',
    commitTime: '5 days ago',
    content:
      '{\n  "name": "my-project",\n  "version": "1.0.0",\n  "scripts": {\n    "dev": "vite",\n    "build": "tsc && vite build",\n    "test": "jest"\n  },\n  "dependencies": {\n    "react": "^18.0.0",\n    "react-dom": "^18.0.0"\n  }\n}\n',
  },
  {
    name: 'README.md',
    type: 'file',
    lastCommit: 'Update README',
    commitTime: '2 days ago',
    content:
      '# My Project\n\nA sample project with reusable components and utilities.\n\n## Installation\n\n```bash\nnpm install\n```\n\n## Usage\n\n```ts\nimport { Button } from "./src";\n\n<Button onClick={() => console.log("clicked!")}>Hello</Button>\n```\n\n## License\n\nMIT\n',
  },
  {
    name: 'tsconfig.json',
    type: 'file',
    lastCommit: 'Configure TypeScript',
    commitTime: '5 days ago',
    content:
      '{\n  "compilerOptions": {\n    "target": "ESNext",\n    "module": "ESNext",\n    "moduleResolution": "bundler",\n    "strict": true,\n    "jsx": "react-jsx",\n    "outDir": "dist"\n  },\n  "include": ["src"]\n}\n',
  },
];

const linuxTree: FileNode[] = [
  {
    name: 'arch',
    type: 'dir',
    lastCommit: 'Update arch configs',
    commitTime: '1 hour ago',
    children: [
      {
        name: 'x86',
        type: 'dir',
        lastCommit: 'x86: fix boot params',
        commitTime: '2 hours ago',
        children: [
          {
            name: 'boot',
            type: 'dir',
            lastCommit: 'boot: cleanup setup code',
            commitTime: '3 hours ago',
            children: [
              {
                name: 'header.S',
                type: 'file',
                lastCommit: 'header: add magic byte check',
                commitTime: '3 hours ago',
                content:
                  '# SPDX-License-Identifier: GPL-2.0\n# Linux boot header\n# See Documentation/x86/boot.rst\n\n.code16\n.section ".bstext", "ax"\n\n\t.globl\tbootsect_start\nbootsect_start:\n\tljmpw\t$BOOTSEG, $start2\n\nstart2:\n\tmovw\t%cs, %ax\n\tmovw\t%ax, %ds\n\tmovw\t%ax, %es\n\tmovw\t%ax, %ss\n',
              },
              {
                name: 'setup.h',
                type: 'file',
                lastCommit: 'setup: define boot params struct',
                commitTime: '4 hours ago',
                content:
                  '#ifndef _BOOT_SETUP_H\n#define _BOOT_SETUP_H\n\n#include <linux/types.h>\n\nstruct boot_params {\n\t__u8  screen_info[64];\n\t__u8  apm_bios_info[20];\n\t__u32 ext_ramdisk_image;\n\t__u32 ext_ramdisk_size;\n\t__u32 ext_cmd_line_ptr;\n};\n\n#endif /* _BOOT_SETUP_H */\n',
              },
            ],
          },
        ],
      },
    ],
  },
  {
    name: 'kernel',
    type: 'dir',
    lastCommit: 'sched: optimize CFS wake-up latency',
    commitTime: '45 min ago',
    children: [
      {
        name: 'sched',
        type: 'dir',
        lastCommit: 'sched: tune wake-up latency',
        commitTime: '1 hour ago',
        children: [
          {
            name: 'core.c',
            type: 'file',
            lastCommit: 'sched/core: remove stale comment',
            commitTime: '1 hour ago',
            content:
              '// SPDX-License-Identifier: GPL-2.0-only\n/*\n * kernel/sched/core.c — Core kernel scheduler code\n */\n#include <linux/sched.h>\n#include <linux/sched/task.h>\n#include <linux/sched/nohz.h>\n\nvoid scheduler_tick(void)\n{\n\tint cpu = smp_processor_id();\n\tstruct rq *rq = cpu_rq(cpu);\n\trq->idle_balance = idle_cpu(cpu);\n\ttrigger_load_balance(rq);\n}\n',
          },
          {
            name: 'fair.c',
            type: 'file',
            lastCommit: 'sched/fair: fix vruntime overflow',
            commitTime: '2 hours ago',
            content:
              '// SPDX-License-Identifier: GPL-2.0\n/*\n * Completely Fair Scheduler (CFS) implementation\n */\n#include "sched.h"\n\nstatic void update_curr(struct cfs_rq *cfs_rq)\n{\n\tstruct sched_entity *curr = cfs_rq->curr;\n\tu64 now = rq_clock_task(rq_of(cfs_rq));\n\tu64 delta_exec = now - curr->exec_start;\n\n\tcurr->vruntime += delta_exec;\n\tcfs_rq->min_vruntime = max_vruntime(\n\t\tcfs_rq->min_vruntime, curr->vruntime);\n}\n',
          },
        ],
      },
      {
        name: 'fork.c',
        type: 'file',
        lastCommit: 'fork: improve clone3 error handling',
        commitTime: '3 hours ago',
        content:
          '// SPDX-License-Identifier: GPL-2.0-only\n/*\n * linux/kernel/fork.c — process creation\n */\n#include <linux/sched.h>\n#include <linux/sched/task.h>\n#include <linux/mm.h>\n\nlong _do_fork(struct kernel_clone_args *args)\n{\n\tstruct task_struct *p;\n\tpid_t nr;\n\n\tp = copy_process(NULL, 0, NUMA_NO_NODE, args);\n\tif (IS_ERR(p))\n\t\treturn PTR_ERR(p);\n\n\tnr = get_task_pid(p, PIDTYPE_PID);\n\twake_up_new_task(p);\n\treturn nr;\n}\n',
      },
    ],
  },
  {
    name: 'Makefile',
    type: 'file',
    lastCommit: 'Makefile: bump version to 6.11',
    commitTime: '2 days ago',
    content:
      '# SPDX-License-Identifier: GPL-2.0\nVERSION = 6\nPATCHLEVEL = 11\nSUBLEVEL = 0\nEXTRAVERSION =\nNAME = Baby Opossum Posse\n\nexport VERSION PATCHLEVEL SUBLEVEL\n\nSRCTREE := $(if $(KBUILD_SRC),$(KBUILD_SRC),$(CURDIR))\ninclude scripts/Kbuild.include\n',
  },
  {
    name: 'COPYING',
    type: 'file',
    lastCommit: 'COPYING: update GPL-2.0 header',
    commitTime: '2 months ago',
    content:
      '  NOTE! This copyright does *not* cover user programs that use kernel\nservices by normal system calls — this is merely considered normal use\nof the kernel, and does *not* fall under the heading of "derived work".\n\n  GNU GENERAL PUBLIC LICENSE\n  Version 2, June 1991\n\n  Copyright (C) 1989, 1991 Free Software Foundation, Inc.\n',
  },
  {
    name: 'README',
    type: 'file',
    lastCommit: 'README: update build instructions',
    commitTime: '1 week ago',
    content:
      'Linux kernel\n============\n\nThere are several guides for kernel developers and users. These guides\ncan be rendered in a number of formats, like HTML and PDF.\n\nIn order to build the documentation, use::\n\n  make htmldocs\n\nor::\n\n  make pdfdocs\n\nTo build the kernel itself::\n\n  make defconfig\n  make -j$(nproc)\n',
  },
];

export const mockFileTrees: Record<string, FileNode[]> = {
  'torvalds/linux': linuxTree,
};

export function getFileTree(owner: string, name: string): FileNode[] {
  return mockFileTrees[`${owner}/${name}`] ?? defaultTree;
}

// ── Issues ────────────────────────────────────────────────────────────────────

export type Issue = {
  id: number;
  title: string;
  author: string;
  openedAt: string;
  status: 'open' | 'closed';
  label: 'bug' | 'enhancement' | 'documentation' | 'question';
};

const defaultIssues: Issue[] = [
  { id: 1,  title: 'Fix memory leak in HTTP connection pool',      author: 'alice',   openedAt: '2 days ago',   status: 'open',   label: 'bug' },
  { id: 2,  title: 'Add dark mode support',                        author: 'bob',     openedAt: '5 days ago',   status: 'open',   label: 'enhancement' },
  { id: 3,  title: 'Update README with installation instructions', author: 'carol',   openedAt: '1 week ago',   status: 'closed', label: 'documentation' },
  { id: 4,  title: 'Crash on startup when config file is missing', author: 'dave',    openedAt: '3 days ago',   status: 'open',   label: 'bug' },
  { id: 5,  title: 'Support for multiple themes',                  author: 'eve',     openedAt: '2 weeks ago',  status: 'closed', label: 'enhancement' },
  { id: 6,  title: 'How do I configure custom plugins?',           author: 'frank',   openedAt: '4 days ago',   status: 'open',   label: 'question' },
  { id: 7,  title: 'Performance regression in v2.1.0',             author: 'grace',   openedAt: '1 day ago',    status: 'open',   label: 'bug' },
  { id: 8,  title: 'Add TypeScript type definitions',              author: 'henry',   openedAt: '3 weeks ago',  status: 'closed', label: 'enhancement' },
  { id: 9,  title: 'Document all public API methods',              author: 'irene',   openedAt: '10 days ago',  status: 'closed', label: 'documentation' },
  { id: 10, title: 'Race condition in async event handler',        author: 'jack',    openedAt: '6 hours ago',  status: 'open',   label: 'bug' },
];

export function getIssues(owner: string, name: string): Issue[] {
  void owner; void name;
  return defaultIssues;
}

// ── Pull Requests ─────────────────────────────────────────────────────────────

export type PullRequest = {
  id: number;
  title: string;
  author: string;
  openedAt: string;
  status: 'open' | 'merged' | 'closed';
  sourceBranch: string;
  targetBranch: string;
};

const defaultPRs: PullRequest[] = [
  { id: 101, title: 'Fix memory leak in connection pool',        author: 'alice',  openedAt: '1 day ago',   status: 'open',   sourceBranch: 'fix/memory-leak',    targetBranch: 'main' },
  { id: 102, title: 'Implement dark mode toggle',               author: 'bob',    openedAt: '3 days ago',  status: 'open',   sourceBranch: 'feat/dark-mode',     targetBranch: 'main' },
  { id: 103, title: 'Update README and contributing guide',     author: 'carol',  openedAt: '1 week ago',  status: 'merged', sourceBranch: 'docs/readme-update', targetBranch: 'main' },
  { id: 104, title: 'Bump all dependencies to latest',          author: 'dave',   openedAt: '5 days ago',  status: 'closed', sourceBranch: 'chore/deps-update',  targetBranch: 'main' },
  { id: 105, title: 'Add TypeScript support across the board',  author: 'henry',  openedAt: '2 weeks ago', status: 'merged', sourceBranch: 'feat/typescript',    targetBranch: 'main' },
  { id: 106, title: 'Cache parsed config files for speed',      author: 'grace',  openedAt: '2 days ago',  status: 'open',   sourceBranch: 'perf/config-cache',  targetBranch: 'develop' },
  { id: 107, title: 'Fix race condition in event handler',      author: 'jack',   openedAt: '5 hours ago', status: 'open',   sourceBranch: 'fix/race-condition', targetBranch: 'main' },
  { id: 108, title: 'Refactor plugin loader to use async/await',author: 'eve',    openedAt: '3 weeks ago', status: 'merged', sourceBranch: 'refactor/plugins',   targetBranch: 'main' },
];

export function getPullRequests(owner: string, name: string): PullRequest[] {
  void owner; void name;
  return defaultPRs;
}

// ── Bounties ──────────────────────────────────────────────────────────────────

export type RepoBounty = {
  id: number;
  issueId: number;
  issueTitle: string;
  amount: number;
  status: 'open' | 'claimed' | 'completed';
  poster: string;
};

const defaultBounties: RepoBounty[] = [
  { id: 1, issueId: 1,  issueTitle: 'Fix memory leak in HTTP connection pool',      amount: 150, status: 'open',      poster: 'alice' },
  { id: 2, issueId: 4,  issueTitle: 'Crash on startup when config file is missing', amount: 75,  status: 'claimed',   poster: 'bob' },
  { id: 3, issueId: 7,  issueTitle: 'Performance regression in v2.1.0',             amount: 200, status: 'open',      poster: 'corp_acme' },
  { id: 4, issueId: 3,  issueTitle: 'Update README with installation instructions', amount: 25,  status: 'completed', poster: 'dave' },
  { id: 5, issueId: 2,  issueTitle: 'Add dark mode support',                        amount: 100, status: 'open',      poster: 'eve' },
  { id: 6, issueId: 10, issueTitle: 'Race condition in async event handler',         amount: 300, status: 'claimed',   poster: 'corp_acme' },
];

export function getBounties(owner: string, name: string): RepoBounty[] {
  void owner; void name;
  return defaultBounties;
}
