import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

/**
 * Reusable form input with validation error states and optional password toggle.
 */
const Input = ({
  label,
  id,
  name,
  type = 'text',
  placeholder = '',
  value,
  onChange,
  error = '',
  helperText = '',
  required = false,
  disabled = false,
  icon: Icon,
  className = '',
  ...props
}) => {
  const [showPassword, setShowPassword] = useState(false);
  const isPassword = type === 'password';
  const inputType = isPassword ? (showPassword ? 'text' : 'password') : type;

  const inputId = id || name;

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={inputId} className="block text-sm font-semibold text-aubergine mb-1.5">
          {label} {required && <span className="text-status-danger">*</span>}
        </label>
      )}

      <div className="relative rounded-xl shadow-sm">
        {Icon && (
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-neutral-500">
            <Icon className="h-4 w-4" />
          </div>
        )}

        <input
          id={inputId}
          name={name}
          type={inputType}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          disabled={disabled}
          required={required}
          className={`
            block w-full rounded-xl text-sm transition duration-150 ease-in-out
            bg-white text-aubergine placeholder:text-neutral-400
            border ${error ? 'border-status-danger focus:ring-status-danger focus:border-status-danger' : 'border-silver-border focus:ring-aubergine focus:border-aubergine'}
            ${Icon ? 'pl-10' : 'pl-3.5'}
            ${isPassword ? 'pr-10' : 'pr-3.5'}
            py-2.5 focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:bg-silver-light disabled:cursor-not-allowed
          `}
          {...props}
        />

        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            tabIndex={-1}
            className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-neutral-500 hover:text-aubergine focus:outline-none"
          >
            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        )}
      </div>

      {error ? (
        <p className="mt-1.5 text-xs text-status-danger font-medium flex items-center gap-1">
          {error}
        </p>
      ) : helperText ? (
        <p className="mt-1.5 text-xs text-neutral-500">{helperText}</p>
      ) : null}
    </div>
  );
};

export default Input;

