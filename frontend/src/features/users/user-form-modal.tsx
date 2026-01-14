'use client';

import { useEffect, useRef, useState } from 'react';
import { Button, FormField } from '@/components';
import type { UserRole } from '@/util/auth';
import type { UserListItem } from './user-list';

export interface UserFormData {
  email: string;
  fullName: string;
  password?: string;
  role: UserRole;
}

interface UserFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: UserFormData) => Promise<void>;
  user?: UserListItem | null;
  isLoading?: boolean;
}

export function UserFormModal({
  isOpen,
  onClose,
  onSubmit,
  user,
  isLoading,
}: UserFormModalProps) {
  const modalRef = useRef<HTMLDivElement>(null);
  const [formData, setFormData] = useState<UserFormData>({
    email: '',
    fullName: '',
    password: '',
    role: 'OFFICER',
  });
  const [errors, setErrors] = useState<Partial<Record<keyof UserFormData, string>>>({});

  const isEditing = !!user;

  useEffect(() => {
    if (user) {
      setFormData({
        email: user.email,
        fullName: user.fullName,
        password: '',
        role: user.roles[0] || 'OFFICER',
      });
    } else {
      setFormData({
        email: '',
        fullName: '',
        password: '',
        role: 'OFFICER',
      });
    }
    setErrors({});
  }, [user, isOpen]);

  useEffect(() => {
    if (isOpen) {
      const handleEscape = (e: KeyboardEvent) => {
        if (e.key === 'Escape') {
          onClose();
        }
      };
      document.addEventListener('keydown', handleEscape);
      return () => document.removeEventListener('keydown', handleEscape);
    }
  }, [isOpen, onClose]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      modalRef.current?.focus();
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  const validate = (): boolean => {
    const newErrors: Partial<Record<keyof UserFormData, string>> = {};

    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Invalid email format';
    }

    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Full name is required';
    }

    if (!isEditing && !formData.password) {
      newErrors.password = 'Password is required for new users';
    } else if (formData.password && formData.password.length < 12) {
      newErrors.password = 'Password must be at least 12 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const submitData: UserFormData = {
      ...formData,
      password: formData.password || undefined,
    };
    await onSubmit(submitData);
  };

  const handleChange = (field: keyof UserFormData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: undefined }));
    }
  };

  if (!isOpen) return null;

  return (
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="user-form-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      onKeyDown={(e) => {
        if (e.key === 'Escape') onClose();
      }}
    >
      <div
        ref={modalRef}
        tabIndex={-1}
        className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 id="user-form-title" className="text-lg font-semibold text-neutral-900">
            {isEditing ? 'Edit User' : 'Create New User'}
          </h2>
          <button
            onClick={onClose}
            className="rounded p-1 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
            aria-label="Close modal"
          >
            <svg
              className="h-5 w-5"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.5}
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <FormField
            label="Full Name"
            id="user-fullname"
            type="text"
            value={formData.fullName}
            onChange={(e) => handleChange('fullName', e.target.value)}
            placeholder="Enter full name"
            disabled={isLoading}
            error={errors.fullName}
            required
          />

          <FormField
            label="Email"
            id="user-email"
            type="email"
            value={formData.email}
            onChange={(e) => handleChange('email', e.target.value)}
            placeholder="Enter email address"
            disabled={isLoading}
            error={errors.email}
            required
          />

          <FormField
            label={isEditing ? 'New Password (leave blank to keep current)' : 'Password'}
            id="user-password"
            type="password"
            value={formData.password || ''}
            onChange={(e) => handleChange('password', e.target.value)}
            placeholder={isEditing ? 'Leave blank to keep current' : 'Enter password'}
            disabled={isLoading}
            error={errors.password}
            required={!isEditing}
            hint="Minimum 12 characters"
            autoComplete="new-password"
          />

          <div className="space-y-1">
            <label
              htmlFor="user-role"
              className="block text-sm font-medium text-neutral-900"
            >
              Role
              <span className="ml-1 text-error" aria-hidden="true">*</span>
              <span className="sr-only">(required)</span>
            </label>
            <select
              id="user-role"
              value={formData.role}
              onChange={(e) => handleChange('role', e.target.value as UserRole)}
              disabled={isLoading}
              required
              className="w-full min-h-touch rounded border-2 border-neutral-400 bg-white px-3 py-2 text-neutral-900 focus:border-primary focus:outline-none focus:ring-focus focus:ring-primary focus:ring-offset-focus disabled:bg-neutral-100 disabled:text-neutral-500"
            >
              <option value="OFFICER">Officer</option>
              <option value="AUDITOR">Auditor</option>
              <option value="ADMINISTRATOR">Administrator</option>
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={isLoading}>
              {isEditing ? 'Save Changes' : 'Create User'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
