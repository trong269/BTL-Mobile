import { Navigate } from 'react-router-dom';
import { useAppContext } from '../context/AppContext';

export default function ProtectedRoute({ children }: { children: JSX.Element }) {
  const { isAuthenticated } = useAppContext();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
}
