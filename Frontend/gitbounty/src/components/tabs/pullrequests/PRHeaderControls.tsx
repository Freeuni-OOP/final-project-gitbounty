import {type Filter, PRIcon} from "../../icons/pullrequest/PRIcons.tsx";

type PRHeaderControlsProps = {
    currentFilter: Filter;
    onFilterChange: (filter: Filter) => void;
    counts: Record<Filter, number>;
    disabled?: boolean;
    onCreatePR?: () => void;
};

export default function PRHeaderControls({
                                             currentFilter,
                                             onFilterChange,
                                             counts,
                                             disabled = false,
                                             onCreatePR,
                                         }: Readonly<PRHeaderControlsProps>) {
    const filters: Filter[] = ['OPEN', 'MERGED', 'CLOSED'];

    return (
        <div className="tab-panel-header-controls">
            <div className="tab-panel-filters">
                {filters.map((f) => (
                    <button
                        key={f}
                        className={`tab-panel-filter ${currentFilter === f ? 'active' : ''}`}
                        onClick={() => onFilterChange(f)}
                        disabled={disabled}
                    >
                        {disabled ? <span className="pr-status-icon" /> : <PRIcon status={f} />}
                        {` ${counts[f]} ${f.charAt(0).toUpperCase() + f.slice(1)}`}
                    </button>
                ))}
            </div>

            {onCreatePR && (
                <button
                    className="btn-create-pr"
                    onClick={onCreatePR}
                    disabled={disabled}
                >
                    New Pull Request
                </button>
            )}
        </div>
    );
}