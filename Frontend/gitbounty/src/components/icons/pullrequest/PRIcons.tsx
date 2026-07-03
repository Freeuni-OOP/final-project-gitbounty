import type {SVGProps} from 'react';

export type PullRequest = {
    id: number;
    number: number;
    title: string;
    description?: string | null;
    authorUsername: string;
    createdAt: string;
    status: 'OPEN' | 'MERGED' | 'CLOSED';
    sourceBranch: string;
    targetBranch: string;
};

export type Filter = 'OPEN' | 'MERGED' | 'CLOSED';

function OpenPRIcon(props: Readonly<SVGProps<SVGSVGElement>>) {
    return (
        <svg className="pr-status-icon open" viewBox="0 0 16 16" aria-hidden="true" {...props}>
            <path d="M1.5 3.25a2.25 2.25 0 1 1 3 2.122v5.256a2.251 2.251 0 1 1-1.5 0V5.372A2.25 2.25 0 0 1 1.5 3.25Zm5.677-.177L9.573.677A.25.25 0 0 1 10 .854V2.5h1A2.5 2.5 0 0 1 13.5 5v5.628a2.251 2.251 0 1 1-1.5 0V5a1 1 0 0 0-1-1h-1v1.646a.25.25 0 0 1-.427.177L7.177 3.427a.25.25 0 0 1 0-.354ZM3.75 2.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm0 9.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm8.25.75a.75.75 0 1 0 1.5 0 .75.75 0 0 0-1.5 0Z" />
        </svg>
    );
}

function MergedPRIcon(props: Readonly<SVGProps<SVGSVGElement>>) {
    return (
        <svg className="pr-status-icon merged" viewBox="0 0 16 16" aria-hidden="true" {...props}>
            <path d="M5.45 5.154A4.25 4.25 0 0 0 9.25 7.5h1.378a2.251 2.251 0 1 1 0 1.5H9.25A5.734 5.734 0 0 1 5 7.123v3.505a2.25 2.25 0 1 1-1.5 0V5.372a2.25 2.25 0 1 1 1.95-.218ZM4.25 13.5a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Zm8.5-4.5a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5ZM5 3.25a.75.75 0 1 0 0 .005V3.25Z" />
        </svg>
    );
}

function ClosedPRIcon(props: Readonly<SVGProps<SVGSVGElement>>) {
    return (
        <svg className="pr-status-icon closed" viewBox="0 0 16 16" aria-hidden="true" {...props}>
            <path d="M3.25 1A2.25 2.25 0 0 1 5 5.372v5.256a2.251 2.251 0 1 1-1.5 0V5.372A2.25 2.25 0 0 1 3.25 1Zm9.5 5.5a.75.75 0 0 1 .75.75v3.378a2.251 2.251 0 1 1-1.5 0V7.25a.75.75 0 0 1 .75-.75Zm-2.03-5.273a.75.75 0 0 1 1.06 0l.97.97.97-.97a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734l-.97.97.97.97a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215l-.97-.97-.97.97a.749.749 0 0 1-1.275-.326.749.749 0 0 1 .215-.734l.97-.97-.97-.97a.75.75 0 0 1 0-1.06ZM3.25 2.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm0 9.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm9.5 0a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Z" />
        </svg>
    );
}

export function PRIcon({ status, ...props }: Readonly<{ status: PullRequest['status'] }> & SVGProps<SVGSVGElement>) {
    if (status === 'MERGED') return <MergedPRIcon {...props} />;
    if (status === 'CLOSED') return <ClosedPRIcon {...props} />;
    return <OpenPRIcon {...props} />;
}