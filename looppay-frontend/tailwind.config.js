/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        aubergine: {
          DEFAULT: '#3A0F34',
          dark: '#2A0B26',
          light: '#53174B',
          50: '#F9F4F8',
          100: '#F1E6EF',
          200: '#DFC2DC',
          800: '#2A0B26',
          900: '#1B0618'
        },
        marigold: {
          DEFAULT: '#EAAA00',
          hover: '#D49A00',
          light: '#FFF5D6',
          50: '#FFFBEA'
        },
        silver: {
          DEFAULT: '#DDE3E8',
          light: '#F4F7F9',
          border: '#CCD5DC',
          dark: '#B0BCC6'
        },
        status: {
          success: '#2E7D5B',
          'success-bg': '#EAF5F0',
          danger: '#B3261E',
          'danger-bg': '#FDF2F2',
          warning: '#EAAA00',
          'warning-bg': '#FFFBEA',
          neutral: '#6B7280',
          'neutral-bg': '#F3F4F6'
        }
      },
      fontFamily: {
        sans: ['Inter', 'Poppins', 'system-ui', '-apple-system', 'sans-serif'],
      },
      borderRadius: {
        'xl': '0.75rem',
        '2xl': '1rem'
      },
      boxShadow: {
        'soft': '0 2px 8px -1px rgba(58, 15, 52, 0.06), 0 1px 3px -1px rgba(58, 15, 52, 0.04)',
        'card': '0 4px 12px -2px rgba(58, 15, 52, 0.05), 0 2px 4px -2px rgba(58, 15, 52, 0.03)',
        'modal': '0 20px 25px -5px rgba(58, 15, 52, 0.2), 0 8px 10px -6px rgba(58, 15, 52, 0.15)'
      }
    },
  },
  plugins: [],
}

