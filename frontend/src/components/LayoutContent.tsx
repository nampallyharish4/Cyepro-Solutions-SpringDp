'use client';

import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { Sidebar } from '@/components/Sidebar';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { supabase } from '@/lib/supabase';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function LayoutContent({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const isLoginPage = pathname === '/login';
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const check = async () => {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session && !isLoginPage) {
        router.replace('/login');
      } else {
        setReady(true);
      }
    };
    check();

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      if (!session && pathname !== '/login') {
        router.replace('/login');
      }
    });
    return () => subscription.unsubscribe();
  }, [pathname, isLoginPage, router]);

  if (!ready && !isLoginPage) return null;

  return (
    <>
      {!isLoginPage && <Sidebar />}
      <main
        className={cn(
          'flex-1 px-4 pt-8 pb-24 md:px-10 md:py-16',
          isLoginPage && 'flex items-center justify-center p-0 pb-0',
        )}
      >
        <div
          className={cn(
            'mx-auto max-w-7xl animate-in fade-in slide-in-from-bottom-5 duration-700',
            isLoginPage && 'w-full max-w-none',
          )}
        >
          {children}
        </div>
      </main>
    </>
  );
}
