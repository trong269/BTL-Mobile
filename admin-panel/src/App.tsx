/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Toaster } from "sonner";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import Dashboard from "./pages/Dashboard";
import Library from "./pages/Library";
import BookChapters from "./pages/BookChapters";
import Categories from "./pages/Categories";
import AIConfig from "./pages/AIConfig";
import Users from "./pages/Users";
import Notifications from "./pages/Notifications";
import Help from "./pages/Help";
import Login from "./pages/Login";
import { AppProvider } from "./context/AppContext";

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Toaster position="top-right" richColors />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="library" element={<Library />} />
            <Route path="library/:bookId/chapters" element={<BookChapters />} />
            <Route path="categories" element={<Categories />} />
            <Route path="ai-config" element={<AIConfig />} />
            <Route path="users" element={<Users />} />
            <Route path="notifications" element={<Notifications />} />
            <Route path="help" element={<Help />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProvider>
  );
}
