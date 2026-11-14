import { Link } from 'react-router-dom';
import Icon from '../components/Icon';

export default function NotFoundPage() {
  return (
    <div className="page-center">
      <div className="empty-state">
        <div className="empty-icon"><Icon name="bookmark" size={18} /></div>
        <h3>This page is not on our shelves</h3>
        <p>The page you are looking for does not exist.</p>
        <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
      </div>
    </div>
  );
}
