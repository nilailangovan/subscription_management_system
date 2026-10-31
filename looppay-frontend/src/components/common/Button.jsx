import React from 'react';

/**
 * Reusable Button component respecting LoopPay contrast rules:
 * - Text on primary (marigold) buttons is strictly aubergine (#3A0F34).
 */
const Button = ({
  children,
  variant = 'primary', // 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost'
  size = 'md',         // 'sm' | 'md' | 'lg'
  type = 'button',
  loading = false,
  disabled = false,
  className = '',
  icon: Icon,
  onClick,
  ...props
}) => {
  const baseStyles = 'inline-flex items-center justify-center font-medium transition duration-150 ease-in-out rounded-xl focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed select-none';

  const variants = {
    // Primary: Marigold background with Aubergine text (contrast rule)
    primary: 'bg-marigold text-aubergine font-semibold hover:bg-marigold-hover focus:ring-marigold active:brightness-95 shadow-sm',
    // Secondary: Aubergine background with white text
    secondary: 'bg-aubergine text-white hover:bg-aubergine-light focus:ring-aubergine active:brightness-95 shadow-sm',
    // Outline: Transparent with Aubergine border & text
    outline: 'border-2 border-aubergine text-aubergine hover:bg-aubergine-50 focus:ring-aubergine',
    // Danger: Crimson red with white text
    danger: 'bg-status-danger text-white hover:bg-red-700 focus:ring-red-500 shadow-sm',
    // Ghost: Subtle hover state
    ghost: 'text-aubergine hover:bg-silver/50 focus:ring-aubergine'
  };

  const sizes = {
    sm: 'text-xs px-3 py-1.5 gap-1.5',
    md: 'text-sm px-4 py-2.5 gap-2',
    lg: 'text-base px-6 py-3 gap-2.5 font-semibold'
  };

  return (
    <button
      type={type}
      disabled={disabled || loading}
      onClick={onClick}
      className={`${baseStyles} ${variants[variant] || variants.primary} ${sizes[size] || sizes.md} ${className}`}
      {...props}
    >
      {loading ? (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      ) : Icon ? (
        <Icon className={size === 'sm' ? 'w-4 h-4' : 'w-4 h-4'} />
      ) : null}
      {children}
    </button>
  );
};

export default Button;

