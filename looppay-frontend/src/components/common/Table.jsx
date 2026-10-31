import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import Button from './Button';

/**
 * Reusable data Table component with silver stripes, borders, and pagination controls.
 *
 * @param {Array} columns [{ header: string, accessor: string | func, render?: (row) => ReactNode, className?: string }]
 * @param {Array} data Array of objects
 * @param {number} page Current 1-based page number
 * @param {number} totalPages Total number of pages
 * @param {function} onPageChange Callback when page changes
 */
const Table = ({
  columns = [],
  data = [],
  page = 1,
  totalPages = 1,
  totalItems = 0,
  pageSize = 10,
  onPageChange,
  loading = false,
  emptyMessage = 'No records found.'
}) => {
  return (
    <div className="bg-white rounded-xl shadow-soft border border-silver-border/80 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-aubergine">
          {/* Header */}
          <thead className="bg-silver-light text-xs font-semibold uppercase tracking-wider text-neutral-600 border-b border-silver-border">
            <tr>
              {columns.map((col, idx) => (
                <th key={idx} scope="col" className={`px-5 py-3.5 ${col.className || ''}`}>
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>

          {/* Body */}
          <tbody className="divide-y divide-silver-border/60">
            {loading ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-12 text-center text-neutral-500">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="w-7 h-7 border-3 border-aubergine border-t-marigold rounded-full animate-spin"></div>
                    <span className="text-xs font-medium">Loading data...</span>
                  </div>
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-12 text-center text-neutral-400">
                  <p className="text-sm font-medium">{emptyMessage}</p>
                </td>
              </tr>
            ) : (
              data.map((row, rowIdx) => (
                <tr
                  key={row.id || rowIdx}
                  className={`transition-colors hover:bg-silver-light/60 ${
                    rowIdx % 2 === 1 ? 'bg-silver/10' : 'bg-white'
                  }`}
                >
                  {columns.map((col, colIdx) => (
                    <td key={colIdx} className={`px-5 py-3.5 ${col.className || ''}`}>
                      {col.render
                        ? col.render(row, rowIdx)
                        : typeof col.accessor === 'function'
                        ? col.accessor(row)
                        : row[col.accessor]}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      {totalPages > 0 && (
        <div className="flex items-center justify-between px-5 py-3.5 bg-silver-light/50 border-t border-silver-border/70 text-xs text-neutral-600">
          <div>
            {totalItems > 0 ? (
              <span>
                Showing <span className="font-semibold text-aubergine">{(page - 1) * pageSize + 1}</span> to{' '}
                <span className="font-semibold text-aubergine">
                  {Math.min(page * pageSize, totalItems)}
                </span>{' '}
                of <span className="font-semibold text-aubergine">{totalItems}</span> results
              </span>
            ) : (
              <span>
                Page <span className="font-semibold text-aubergine">{page}</span> of{' '}
                <span className="font-semibold text-aubergine">{totalPages}</span>
              </span>
            )}
          </div>

          <div className="flex items-center gap-1.5">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 1}
              onClick={() => onPageChange && onPageChange(page - 1)}
              className="py-1 px-2.5 text-xs bg-white border-silver-border"
            >
              <ChevronLeft className="w-3.5 h-3.5" /> Previous
            </Button>

            <span className="px-3 py-1 bg-white border border-silver-border rounded-lg text-xs font-semibold text-aubergine shadow-2xs">
              {page} / {totalPages}
            </span>

            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages}
              onClick={() => onPageChange && onPageChange(page + 1)}
              className="py-1 px-2.5 text-xs bg-white border-silver-border"
            >
              Next <ChevronRight className="w-3.5 h-3.5" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Table;

