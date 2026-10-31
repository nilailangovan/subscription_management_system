import React from 'react';

/**
 * Native select styled to match the Input component.
 *
 * @param {Array} options [{ value, label }]
 * @param {string} placeholder Optional empty first option (value '')
 */
const Select = ({
  label,
  id,
  name,
  value,
  onChange,
  options = [],
  placeholder,
  error = '',
  required = false,
  disabled = false,
  className = '',
}) => {
  const selectId = id || name;

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={selectId} className="block text-sm font-semibold text-aubergine mb-1.5">
          {label} {required && <span className="text-status-danger">*</span>}
        </label>
      )}

      <select
        id={selectId}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        className={`
          block w-full rounded-xl text-sm bg-white text-aubergine shadow-sm px-3.5 py-2.5
          border ${error ? 'border-status-danger focus:ring-status-danger' : 'border-silver-border focus:ring-aubergine focus:border-aubergine'}
          focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:bg-silver-light disabled:cursor-not-allowed
        `}
      >
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>

      {error && <p className="mt-1.5 text-xs text-status-danger font-medium">{error}</p>}
    </div>
  );
};

export default Select;
