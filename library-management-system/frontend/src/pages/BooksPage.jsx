import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { booksApi, meApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import BookCover from '../components/BookCover';
import BookFormModal from '../components/BookFormModal';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import Spinner from '../components/Spinner';
import Icon from '../components/Icon';
import Availability from '../components/Availability';
import { formatDate } from '../utils/format';

const PAGE_SIZE = 12;

export default function BooksPage() {
  const { isStaff } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [category, setCategory] = useState('');
  const [sort, setSort] = useState('title');
  const [page, setPage] = useState(0);

  const [result, setResult] = useState(null);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [busyBookId, setBusyBookId] = useState(null);
  const [details, setDetails] = useState(null);
  const [adding, setAdding] = useState(false);

  // Wait until the user stops typing before searching
  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedQuery(query);
      setPage(0);
    }, 300);
    return () => clearTimeout(t);
  }, [query]);

  const loadBooks = useCallback(() => {
    setLoading(true);
    const [field, dir] = sort.split(':');
    booksApi
      .search({ q: debouncedQuery || undefined, category: category || undefined, page, size: PAGE_SIZE, sort: field, dir: dir || 'asc' })
      .then((data) => {
        setResult(data);
        setError('');
      })
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [debouncedQuery, category, sort, page]);

  useEffect(loadBooks, [loadBooks]);
  useEffect(() => {
    booksApi.categories().then(setCategories).catch(() => {});
  }, []);

  const act = async (book) => {
    setBusyBookId(book.id);
    try {
      if (book.availability.available > 0) {
        const loan = await meApi.borrow(book.id);
        toast(`You borrowed "${loan.bookTitle}". Due back on ${formatDate(loan.dueDate)}.`);
      } else {
        const hold = await meApi.placeHold(book.id);
        toast(`Hold placed on "${hold.bookTitle}". You're number ${hold.queuePosition} in line.`);
      }
      setDetails(null);
      loadBooks();
    } catch (err) {
      toast(getErrorMessage(err), 'error');
    } finally {
      setBusyBookId(null);
    }
  };

  const books = result?.content || [];

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Catalogue</h1>
          <p className="page-meta">{isStaff ? 'Titles, copies and who is waiting for them' : 'Borrow what is on the shelf, or join the queue for what is out'}</p>
        </div>
        {isStaff && (
          <button className="btn btn-primary" onClick={() => setAdding(true)}><Icon name="plus" />Add title</button>
        )}
      </div>

      <div className="toolbar">
        <label className="search-field">
          <Icon name="search" />
          <input type="search" placeholder="Title, author or ISBN" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search books" />
        </label>
        <select value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }} aria-label="Category">
          <option value="">All categories</option>
          {categories.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <select value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }} aria-label="Sort by">
          <option value="title">Title A–Z</option>
          <option value="author">Author A–Z</option>
          <option value="publishedYear:desc">Newest editions</option>
          <option value="createdAt:desc">Recently added</option>
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading && !result ? (
        <Spinner />
      ) : books.length === 0 ? (
        <EmptyState title="No books found" icon="search">Try a different search or category.</EmptyState>
      ) : (
        <>
          <p className="results-count">
            {result.totalElements} title{result.totalElements === 1 ? '' : 's'}
            {debouncedQuery && <> matching “{debouncedQuery}”</>}
          </p>
          <div className={`book-grid ${loading ? 'is-loading' : ''}`}>
            {books.map((book, i) => (
              <article key={book.id} className="book-card" style={{ '--i': i }}>
                <button
                  className="book-card-cover"
                  onClick={() => (isStaff ? navigate(`/books/${book.id}`) : setDetails(book))}
                  aria-label={`Details for ${book.title}`}
                >
                  <BookCover title={book.title} author={book.author} />
                </button>
                <div className="book-card-body">
                  {book.category && <span className="book-category">{book.category}</span>}
                  <h3 onClick={() => (isStaff ? navigate(`/books/${book.id}`) : setDetails(book))}>{book.title}</h3>
                  <p className="book-meta">{book.author}{book.publishedYear ? ` · ${book.publishedYear}` : ''}</p>
                  <Availability availability={book.availability} />
                </div>
                <div className="book-card-actions">
                  {isStaff ? (
                    <Link to={`/books/${book.id}`} className="btn btn-secondary btn-sm btn-block">Manage copies</Link>
                  ) : (
                    <MemberAction book={book} busy={busyBookId === book.id} onAct={act} />
                  )}
                </div>
              </article>
            ))}
          </div>

          {result.totalPages > 1 && (
            <div className="pagination">
              <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}><Icon name="chevronLeft" />Previous</button>
              <span>Page {page + 1} of {result.totalPages}</span>
              <button className="btn btn-ghost btn-sm" disabled={result.last} onClick={() => setPage((p) => p + 1)}>Next<Icon name="chevronRight" /></button>
            </div>
          )}
        </>
      )}

      {details && (
        <Modal
          title={details.title}
          onClose={() => setDetails(null)}
          footer={<MemberAction book={details} busy={busyBookId === details.id} onAct={act} primary />}
        >
          <div className="book-details">
            <BookCover title={details.title} author={details.author} />
            <dl>
              <dt>Author</dt><dd>{details.author}</dd>
              <dt>ISBN</dt><dd className="mono">{details.isbn}</dd>
              {details.category && (<><dt>Category</dt><dd>{details.category}</dd></>)}
              {details.publishedYear && (<><dt>Published</dt><dd>{details.publishedYear}</dd></>)}
              <dt>Copies</dt><dd><Availability availability={details.availability} /></dd>
            </dl>
          </div>
          {details.description && <p className="book-description">{details.description}</p>}
        </Modal>
      )}

      {adding && (
        <BookFormModal
          categories={categories}
          onClose={() => setAdding(false)}
          onSaved={(saved) => {
            toast(`"${saved.title}" added with ${saved.availability.total} cop${saved.availability.total === 1 ? 'y' : 'ies'}`);
            navigate(`/books/${saved.id}`);
          }}
        />
      )}
    </>
  );
}

function MemberAction({ book, busy, onAct, primary = false }) {
  const { available, total } = book.availability;
  const cls = `btn ${primary ? 'btn-primary' : 'btn-secondary btn-sm btn-block'}`;
  if (total === 0) {
    return <button className={cls} disabled>Not in circulation</button>;
  }
  if (available > 0) {
    return <button className={cls} disabled={busy} onClick={() => onAct(book)}>{busy ? 'Borrowing…' : primary ? 'Borrow this book' : 'Borrow'}</button>;
  }
  return (
    <button className={cls} disabled={busy} onClick={() => onAct(book)}>
      <Icon name="clock" />
      {busy ? 'Placing hold…' : 'Place hold'}
    </button>
  );
}
