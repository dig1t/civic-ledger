import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      // USWDS-inspired high-contrast color palette for Section 508 compliance
      colors: {
        // Primary colors (high contrast ratios)
        primary: {
          DEFAULT: '#005ea2', // USWDS primary blue
          dark: '#1a4480',
          darker: '#162e51',
          light: '#73b3e7',
          lighter: '#d9e8f6',
        },
        // Secondary/accent colors
        accent: {
          DEFAULT: '#d83933',
          dark: '#b50909',
          light: '#f2938c',
        },
        // Neutral grays with sufficient contrast
        neutral: {
          900: '#1b1b1b', // Primary text
          800: '#2d2d2d',
          700: '#454545',
          600: '#565656',
          500: '#71767a', // Secondary text (meets WCAG AA for large text)
          400: '#a9aeb1',
          300: '#c6cace',
          200: '#dfe1e2',
          100: '#f0f0f0',
          50: '#f8f8f8',
        },
        // Semantic colors
        success: {
          DEFAULT: '#00a91c',
          dark: '#008817',
          light: '#70e17b',
          lighter: '#ecf3ec',
        },
        warning: {
          DEFAULT: '#e5a000',
          dark: '#936f38',
          light: '#fee685',
          lighter: '#faf3d1',
        },
        error: {
          DEFAULT: '#d54309',
          dark: '#b50909',
          light: '#f39268',
          lighter: '#f4e3db',
        },
        info: {
          DEFAULT: '#00bde3',
          dark: '#009ec1',
          light: '#99deea',
          lighter: '#e7f6f8',
        },
      },
      // Enhanced focus ring for keyboard navigation
      ringWidth: {
        focus: '3px',
      },
      ringOffsetWidth: {
        focus: '2px',
      },
      // Minimum touch target size (44x44px for WCAG)
      minHeight: {
        touch: '44px',
      },
      minWidth: {
        touch: '44px',
      },
      // Font sizes that meet accessibility guidelines
      fontSize: {
        // Base minimum of 16px for body text
        base: ['1rem', { lineHeight: '1.625' }],
        sm: ['0.875rem', { lineHeight: '1.5' }],
        lg: ['1.125rem', { lineHeight: '1.625' }],
        xl: ['1.25rem', { lineHeight: '1.5' }],
        '2xl': ['1.5rem', { lineHeight: '1.375' }],
        '3xl': ['1.875rem', { lineHeight: '1.25' }],
        '4xl': ['2.25rem', { lineHeight: '1.2' }],
      },
    },
  },
  plugins: [],
};

export default config;
