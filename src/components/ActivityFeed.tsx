import React from 'react';

interface ActivityFeedProps {
  activities: any[];
}

const ActivityFeed: React.FC<ActivityFeedProps> = ({ activities }) => {
  return (
    <div className="activity-feed">
      {activities.map((activity) => (
        <div key={activity.id} className="activity-info">
          <p>
            {activity.username} {activity.action}
          </p>
        </div>
      ))}
    </div>
  );
};

export default ActivityFeed;