import React from 'react';
import AppHeader from '../components/AppHeader';

interface PageLayoutProps {
  children: React.ReactNode;
}

export default function PageLayout({ children }: PageLayoutProps) {
  return (
    <>
      <AppHeader />
      <div className="container-fluid py-4" style={{ minHeight: '100vh' }}>
        {children}
      </div>
    </>
  );
}
