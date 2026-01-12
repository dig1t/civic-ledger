'use client';

import { type ReactNode, useId } from 'react';
import { cn } from '@/lib/utils';
import { Input, type InputProps } from './input';

export interface FormFieldProps extends Omit<InputProps, 'error'> {
  label: string;
  error?: string;
  hint?: string;
  required?: boolean;
  labelClassName?: string;
  wrapperClassName?: string;
}

export function FormField({
  label,
  error,
  hint,
  required,
  labelClassName,
  wrapperClassName,
  className,
  id: providedId,
  ...inputProps
}: FormFieldProps) {
  const generatedId = useId();
  const id = providedId || generatedId;
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;

  const describedBy = [error ? errorId : null, hint ? hintId : null]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={cn('space-y-1', wrapperClassName)}>
      <label
        htmlFor={id}
        className={cn('block text-sm font-medium text-neutral-900', labelClassName)}
      >
        {label}
        {required && (
          <span className="ml-1 text-error" aria-hidden="true">
            *
          </span>
        )}
        {required && <span className="sr-only">(required)</span>}
      </label>

      {hint && !error && (
        <p id={hintId} className="text-sm text-neutral-500">
          {hint}
        </p>
      )}

      <Input
        id={id}
        error={!!error}
        required={required}
        aria-describedby={describedBy || undefined}
        className={className}
        {...inputProps}
      />

      {error && (
        <p id={errorId} className="text-sm text-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
