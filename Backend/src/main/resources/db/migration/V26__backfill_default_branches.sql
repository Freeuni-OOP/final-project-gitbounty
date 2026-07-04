INSERT INTO branches (codebase_id, name, latest_commit_id, updated_at)
SELECT c.id, 'refs/heads/main', NULL, NOW()
FROM codebases c
WHERE NOT EXISTS (
    SELECT 1
    FROM branches b
    WHERE b.codebase_id = c.id
      AND b.name = 'refs/heads/main'
);