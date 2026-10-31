import React from 'react';

/**
 * Reusable Card component:
 * - White card surface placed on the silver background.
 * - Rounded-xl corners with soft shadows.
 */
const Card = ({
  children,
  title,
  subtitle,
  action,
  className = '',
  bodyClassName = 'p-6',
  headerClassName = 'p-6 pb-0',
  footer,
  footerClassName = 'p-6 pt-4 border-t border-silver/60 bg-silver-light/40 rounded-b-xl',
  ...props
}) => {
  return (
    <div
      className={`bg-white rounded-xl shadow-soft border border-silver-border/60 transition duration-150 ${className}`}
      {...props}
    >
      {(title || subtitle || action) && (
        <div className={`flex items-start justify-between gap-4 ${headerClassName}`}>
          <div>
            {title && <h3 className="text-lg font-bold text-aubergine">{title}</h3>}
            {subtitle && <p className="text-xs text-neutral-500 mt-0.5">{subtitle}</p>}
          </div>
          {action && <div className="flex-shrink-0">{action}</div>}
        </div>
      )}

      <div className={bodyClassName}>{children}</div>

      {footer && <div className={footerClassName}>{footer}</div>}
    </div>
  );
};

export default Card;

