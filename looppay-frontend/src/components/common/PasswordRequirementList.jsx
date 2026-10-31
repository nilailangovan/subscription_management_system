import React from 'react';
import { Check, X } from 'lucide-react';
import { validatePassword } from '../../utils/validators';

/**
 * Visual helper showing real-time password requirements matching RULES.md.
 */
const PasswordRequirementList = ({ password }) => {
  if (!password) return null;

  const { criteria } = validatePassword(password);

  const items = [
    { label: 'More than 8 characters', met: criteria.minLength },
    { label: 'At least 1 uppercase letter (A-Z)', met: criteria.hasUpper },
    { label: 'At least 1 lowercase letter (a-z)', met: criteria.hasLower },
    { label: 'At least 1 special character (!@#$%^&*)', met: criteria.hasSpecial },
  ];

  return (
    <div className="mt-2 p-3 bg-silver-light rounded-xl border border-silver-border/70 text-xs space-y-1.5">
      <p className="font-semibold text-aubergine text-[11px] uppercase tracking-wider mb-1">
        Password Requirements:
      </p>
      {items.map((item, idx) => (
        <div key={idx} className="flex items-center gap-2">
          {item.met ? (
            <Check className="w-3.5 h-3.5 text-status-success flex-shrink-0" />
          ) : (
            <X className="w-3.5 h-3.5 text-status-danger flex-shrink-0" />
          )}
          <span className={item.met ? 'text-status-success font-medium' : 'text-neutral-500'}>
            {item.label}
          </span>
        </div>
      ))}
    </div>
  );
};

export default PasswordRequirementList;

