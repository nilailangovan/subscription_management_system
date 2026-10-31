import React, { useState, useCallback, useRef, useEffect } from 'react';
import { CheckCircle, AlertCircle, X } from 'lucide-react';

/**
 * Lightweight per-page toast.
 * Usage: const { toast, showToast } = useToast(); render {toast} once, then call showToast(message, 'success' | 'error').
 */
export const useToast = () => {
  const [current, setCurrent] = useState(null);
  const timerRef = useRef(null);

  const showToast = useCallback((message, type = 'success') => {
    clearTimeout(timerRef.current);
    setCurrent({ message, type });
    timerRef.current = setTimeout(() => setCurrent(null), 4500);
  }, []);

  // Clear any pending timer when the page unmounts
  useEffect(() => () => clearTimeout(timerRef.current), []);

  const isError = current?.type === 'error';

  // The live region stays mounted so screen readers announce each new message
  const toast = (
    <div aria-live="polite" className="fixed bottom-6 right-6 z-[60] max-w-sm">
      {current && (
        <div
          role={isError ? 'alert' : 'status'}
          className={`flex items-start gap-2.5 p-4 rounded-xl shadow-modal border text-xs font-semibold ${
            isError
              ? 'bg-status-danger-bg border-status-danger/30 text-status-danger'
              : 'bg-status-success-bg border-status-success/30 text-status-success'
          }`}
        >
          {isError ? <AlertCircle className="w-4 h-4 flex-shrink-0" /> : <CheckCircle className="w-4 h-4 flex-shrink-0" />}
          <span className="flex-1 leading-relaxed">{current.message}</span>
          <button
            onClick={() => setCurrent(null)}
            aria-label="Dismiss notification"
            className="p-0.5 rounded hover:bg-black/5 focus:outline-none"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}
    </div>
  );

  return { toast, showToast };
};

/**
 * Reads the backend ErrorResponseDto message from an axios error.
 */
export const apiErrorMessage = (err, fallback) => err.response?.data?.message || fallback;
