/**
 * Password validation according to LoopPay RULES.md:
 * - More than 8 characters (length >= 9)
 * - At least 1 uppercase letter
 * - At least 1 lowercase letter
 * - At least 1 special character (!@#$%^&*)
 */
export const validatePassword = (password) => {
  const minLength = password.length > 8;
  const hasUpper = /[A-Z]/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasSpecial = /[!@#$%^&*]/.test(password);

  const isValid = minLength && hasUpper && hasLower && hasSpecial;

  let errorMessage = '';
  if (!isValid) {
    if (!minLength) {
      errorMessage = 'Password must be more than 8 characters.';
    } else if (!hasUpper) {
      errorMessage = 'Password must include at least one uppercase letter.';
    } else if (!hasLower) {
      errorMessage = 'Password must include at least one lowercase letter.';
    } else if (!hasSpecial) {
      errorMessage = 'Password must include at least one special character (!@#$%^&*).';
    }
  }

  return {
    isValid,
    errorMessage,
    criteria: {
      minLength,
      hasUpper,
      hasLower,
      hasSpecial
    }
  };
};

/**
 * Standard email format validator.
 */
export const validateEmail = (email) => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(String(email).toLowerCase());
};

