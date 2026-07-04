export type IssueStatus =
    | 'OPEN'
    | 'IN_PROGRESS'
    | 'CLOSED';

export interface IssueAPI {
    id: number;
    number: number;
    title: string;
    description: string | null;
    status: IssueStatus;
    authorId: number;
    authorUsername: string;
    assignedToId: number | null;
    assignedToUsername: string | null;
    repositoryId: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateIssueRequest {
    title: string;
    description: string | null;
}