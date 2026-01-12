import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, type = 'text', error, ...props }, ref) => {
    return (
      <input
        ref={ref}
        type={type}
        className={cn(
          'min-h-touch w-full rounded border-2 bg-white px-3 py-2 text-neutral-900 placeholder:text-neutral-500',
          'focus:outline-none focus:ring-focus focus:ring-offset-focus',
          'disabled:bg-neutral-100 disabled:text-neutral-500',
          error
            ? 'border-error focus:border-error focus:ring-error'
            : 'border-neutral-400 focus:border-primary focus:ring-primary',
          className
        )}
        aria-invalid={error ? 'true' : undefined}
        {...props}
      />
    );
  }
);

Input.displayName = 'Input';

export { Input };
