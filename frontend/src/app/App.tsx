import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import type { Dependencies } from '@/di/container';
import { AppShell } from '@/presentation/layouts/AppShell';
import { DashboardPage } from '@/presentation/pages/DashboardPage';
import { ForgotPasswordPage } from '@/presentation/pages/ForgotPasswordPage';
import { LabsPage } from '@/presentation/pages/LabsPage';
import { LoginPage } from '@/presentation/pages/LoginPage';
import { ModulePlaceholderPage } from '@/presentation/pages/ModulePlaceholderPage';
import { NotFoundPage } from '@/presentation/pages/NotFoundPage';
import { RegisterPage } from '@/presentation/pages/RegisterPage';
import { ResetPasswordPage } from '@/presentation/pages/ResetPasswordPage';
import { SettingsPage } from '@/presentation/pages/SettingsPage';
import { VmDetailPage } from '@/presentation/pages/VmDetailPage';
import { VpnPage } from '@/presentation/pages/VpnPage';
import { GuestRoute, ProtectedRoute } from '@/presentation/routing/guards';
import { AuthProvider } from '@/presentation/state/AuthContext';
import { DependenciesProvider } from '@/presentation/state/DependenciesContext';

export function App({ dependencies }: { dependencies: Dependencies }) {
  return (
    <DependenciesProvider value={dependencies}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<GuestRoute />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            </Route>
            {/* Accessible connecté ou non : le lien arrive par e-mail. */}
            <Route path="/reset-password" element={<ResetPasswordPage />} />

            <Route element={<ProtectedRoute />}>
              <Route element={<AppShell />}>
                <Route index element={<DashboardPage />} />
                <Route path="/dashboard" element={<Navigate to="/" replace />} />
                <Route path="/labs" element={<LabsPage />} />
                <Route path="/labs/:id" element={<VmDetailPage />} />
                <Route path="/vpn" element={<VpnPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="/modules/:moduleId" element={<ModulePlaceholderPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </DependenciesProvider>
  );
}
