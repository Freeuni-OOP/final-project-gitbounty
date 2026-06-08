import React from 'react';

interface TrendingTopicsProps {
  topics: string[];
}

const TrendingTopics: React.FC<TrendingTopicsProps> = ({ topics }) => {
  return (
    <div className="trending-topics">
      {topics.map((topic) => (
        <span key={topic} className="topic-pill">
          {topic}
        </span>
      ))}
    </div>
  );
};

export default TrendingTopics;