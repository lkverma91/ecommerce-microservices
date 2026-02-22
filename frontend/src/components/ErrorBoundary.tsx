import React from 'react';

interface Props {
  error?: string;
  onRetry?: () => void;
}

export const ErrorDisplay: React.FC<Props> = ({ error, onRetry }) => (
  <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-center">
    <p className="text-red-700">{error || 'Something went wrong.'}</p>
    {onRetry && (
      <button
        onClick={onRetry}
        className="mt-2 rounded bg-red-100 px-4 py-2 text-red-800 hover:bg-red-200"
      >
        Retry
      </button>
    )}
  </div>
);
