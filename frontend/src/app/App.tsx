import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import type { Dependencies } from '@/di/container';
import { AppShell } from '@/presentation/layouts/AppShell';
import { AdminBoxesPage } from '@/presentation/pages/AdminBoxesPage';
import { AdminCourseEditorPage } from '@/presentation/pages/AdminCourseEditorPage';
import { AdminCoursesPage } from '@/presentation/pages/AdminCoursesPage';
import { AdminScenariosPage } from '@/presentation/pages/AdminScenariosPage';
import { AdminSupportPage } from '@/presentation/pages/AdminSupportPage';
import { AttackPathsPage } from '@/presentation/pages/AttackPathsPage';
import { AdminDashboardPage } from '@/presentation/pages/AdminDashboardPage';
import { BoxDetailPage } from '@/presentation/pages/BoxDetailPage';
import { CourseDetailPage } from '@/presentation/pages/CourseDetailPage';
import { CoursesPage } from '@/presentation/pages/CoursesPage';
import { DashboardPage } from '@/presentation/pages/DashboardPage';
import { EventsPage } from '@/presentation/pages/EventsPage';
import { ExposurePage } from '@/presentation/pages/ExposurePage';
import { ForgotPasswordPage } from '@/presentation/pages/ForgotPasswordPage';
import { LabsPage } from '@/presentation/pages/LabsPage';
import { LoginPage } from '@/presentation/pages/LoginPage';
import { MachinesPage } from '@/presentation/pages/MachinesPage';
import { NotFoundPage } from '@/presentation/pages/NotFoundPage';
import { PaymentReturnPage } from '@/presentation/pages/PaymentReturnPage';
import { RegisterPage } from '@/presentation/pages/RegisterPage';
import { ReportCenterPage } from '@/presentation/pages/ReportCenterPage';
import { ProfilePage } from '@/presentation/pages/ProfilePage';
import { ResetPasswordPage } from '@/presentation/pages/ResetPasswordPage';
import { ScenariosPage } from '@/presentation/pages/ScenariosPage';
import { ScoreboardPage } from '@/presentation/pages/ScoreboardPage';
import { SettingsPage } from '@/presentation/pages/SettingsPage';
import { SubscriptionPage } from '@/presentation/pages/SubscriptionPage';
import { SupportPage } from '@/presentation/pages/SupportPage';
import { VmDetailPage } from '@/presentation/pages/VmDetailPage';
import { VpnPage } from '@/presentation/pages/VpnPage';
import { I18nProvider } from '@/presentation/i18n/I18nContext';
import { AdminRoute, GuestRoute, PlayerRoute, ProtectedRoute } from '@/presentation/routing/guards';
import { AuthProvider } from '@/presentation/state/AuthContext';
import { ThemeProvider } from '@/presentation/theme/ThemeContext';
import { DependenciesProvider } from '@/presentation/state/DependenciesContext';

export function App({ dependencies }: { dependencies: Dependencies }) {
  return (
    <DependenciesProvider value={dependencies}>
      {/* Langue et thème enveloppent tout : ils valent aussi sur la page de
          connexion, avant qu'il y ait un compte à interroger. */}
      <ThemeProvider>
        <I18nProvider>
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
                    {/* Réglages du compte : communs aux deux rôles. */}
                    <Route path="/settings" element={<SettingsPage />} />

                    {/* Pages de joueur : un administrateur y est renvoyé vers /admin. */}
                    <Route element={<PlayerRoute />}>
                      <Route index element={<DashboardPage />} />
                      <Route path="/dashboard" element={<Navigate to="/" replace />} />
                      <Route path="/machines" element={<MachinesPage />} />
                      <Route path="/machines/:slug" element={<BoxDetailPage />} />
                      <Route path="/scoreboard" element={<ScoreboardPage />} />
                      {/* Sans filière : tout le catalogue. Avec : elle est présélectionnée,
                        ce qui garde valides les liens en /cours/:filiere/:cours. */}
                    <Route path="/cours" element={<CoursesPage />} />
                    <Route path="/cours/:track" element={<CoursesPage />} />
                      <Route path="/cours/:track/:slug" element={<CourseDetailPage />} />
                      <Route path="/profil" element={<ProfilePage />} />
                      <Route path="/abonnement" element={<SubscriptionPage />} />
                      <Route path="/abonnement/retour" element={<PaymentReturnPage />} />
                      <Route path="/labs" element={<LabsPage />} />
                      <Route path="/labs/:id" element={<VmDetailPage />} />
                      <Route path="/vpn" element={<VpnPage />} />
                      <Route path="/modules/events" element={<EventsPage />} />
                      <Route path="/modules/support" element={<SupportPage />} />
                      <Route path="/modules/exposure-analysis" element={<ExposurePage />} />
                      <Route path="/modules/attack-paths" element={<AttackPathsPage />} />
                      <Route path="/modules/report-center" element={<ReportCenterPage />} />
                      <Route path="/modules/scenario-designer" element={<ScenariosPage />} />
                      <Route path="/modules/scenario-designer/:slug" element={<ScenariosPage />} />
                    </Route>

                    <Route element={<AdminRoute />}>
                      <Route path="/admin" element={<AdminDashboardPage />} />
                      <Route path="/admin/cours" element={<AdminCoursesPage />} />
                      <Route path="/admin/machines" element={<AdminBoxesPage />} />
                      <Route path="/admin/cours/:slug" element={<AdminCourseEditorPage />} />
                      <Route path="/admin/support" element={<AdminSupportPage />} />
                      <Route path="/admin/scenarios" element={<AdminScenariosPage />} />
                    </Route>

                    <Route path="*" element={<NotFoundPage />} />
                  </Route>
                </Route>
              </Routes>
            </BrowserRouter>
          </AuthProvider>
        </I18nProvider>
      </ThemeProvider>
    </DependenciesProvider>
  );
}
