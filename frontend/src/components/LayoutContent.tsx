'use client';

import { usePathname } from 'next/navigation';
import { Sidebar } from '@/components/Sidebar';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function LayoutContent({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  // No longer checking for auth session or redirecting to /login

  return (
    <>
      <Sidebar />
      <main
        className={cn(
          'flex-1 px-4 pt-8 pb-24 md:px-10 md:py-16'
        )}
      >
        <div
          className={cn(
            'mx-auto max-w-7xl animate-in fade-in slide-in-from-bottom-5 duration-700'
          )}
        >
          {children}
        </div>
      </main>
    </>
  );
}
