import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, BookOpen, Loader2, Plus, Save, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import type { ChapterDto, ChapterPayload } from '../api/chaptersApi';
import { useBookChapters, useBooks, useCreateChapter, useDeleteChapter, useUpdateChapter } from '../api/queries';
import { normalizeRichText } from '../lib/utils';

const EMPTY_FORM: ChapterPayload = {
  chapterNumber: 1,
  title: '',
  content: '',
};

function toChapterPayload(chapter: ChapterDto): ChapterPayload {
  return {
    chapterNumber: chapter.chapterNumber,
    title: chapter.title,
    content: chapter.content,
  };
}

export default function BookChapters() {
  const navigate = useNavigate();
  const { bookId = '' } = useParams();
  const { data: books = [], isLoading: isLoadingBooks } = useBooks();
  const { data: chapters = [], isLoading, isError, error, refetch } = useBookChapters(bookId || undefined);
  const createChapterMutation = useCreateChapter();
  const updateChapterMutation = useUpdateChapter();
  const deleteChapterMutation = useDeleteChapter();
  const [editingChapterId, setEditingChapterId] = useState<string | null>(null);
  const [form, setForm] = useState<ChapterPayload>(EMPTY_FORM);

  const book = useMemo(() => books.find((item) => item.id === bookId) || null, [bookId, books]);
  const isMutating = createChapterMutation.isPending || updateChapterMutation.isPending || deleteChapterMutation.isPending;

  const resetForm = () => {
    setEditingChapterId(null);
    setForm({
      chapterNumber: chapters.length + 1,
      title: '',
      content: '',
    });
  };

  const selectChapter = (chapter: ChapterDto) => {
    setEditingChapterId(chapter.id);
    setForm(toChapterPayload(chapter));
  };

  const handleCreateClick = () => {
    resetForm();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookId) return;

    const payload = {
      chapterNumber: Number(form.chapterNumber),
      title: form.title.trim(),
      content: form.content,
    } satisfies ChapterPayload;

    if (!payload.title) {
      toast.error('Vui lòng nhập tiêu đề chương.');
      return;
    }

    if (!payload.chapterNumber || payload.chapterNumber < 1) {
      toast.error('Số chương phải lớn hơn 0.');
      return;
    }

    try {
      if (editingChapterId) {
        await updateChapterMutation.mutateAsync({ bookId, chapterId: editingChapterId, payload });
        toast.success('Đã cập nhật chương.');
      } else {
        await createChapterMutation.mutateAsync({ bookId, payload });
        toast.success('Đã thêm chương mới.');
      }
      resetForm();
    } catch {
      toast.error('Không thể lưu chương. Vui lòng thử lại.');
    }
  };

  const handleDelete = async () => {
    if (!bookId || !editingChapterId) return;

    try {
      await deleteChapterMutation.mutateAsync({ bookId, chapterId: editingChapterId });
      toast.success('Đã xóa chương.');
      resetForm();
    } catch {
      toast.error('Không thể xóa chương. Vui lòng thử lại.');
    }
  };

  if (!bookId) {
    return <div className="p-8 text-center text-on-surface-variant">Thiếu mã sách.</div>;
  }

  if (isLoadingBooks || isLoading) {
    return (
      <div className="p-8 flex items-center justify-center gap-3 text-on-surface-variant">
        <Loader2 className="w-5 h-5 animate-spin" />
        Đang tải dữ liệu chương...
      </div>
    );
  }

  if (!book) {
    return (
      <div className="p-8 space-y-4 text-center">
        <p className="text-on-surface-variant">Không tìm thấy sách.</p>
        <button
          type="button"
          onClick={() => navigate('/library')}
          className="px-4 py-2 rounded-xl bg-primary text-on-primary text-sm font-medium hover:bg-secondary transition-colors"
        >
          Quay lại thư viện
        </button>
      </div>
    );
  }

  const previewContent = normalizeRichText(form.content);

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="space-y-2">
          <Link to="/library" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:text-secondary transition-colors">
            <ArrowLeft className="w-4 h-4" />
            Quay lại thư viện
          </Link>
          <div>
            <h1 className="text-3xl font-serif font-semibold text-on-surface">Quản lý chương</h1>
            <p className="text-on-surface-variant mt-1">{book.title} • {book.author}</p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleCreateClick}
          className="inline-flex items-center justify-center gap-2 rounded-full bg-primary px-5 py-2.5 font-medium text-on-primary transition-colors hover:bg-secondary"
        >
          <Plus className="w-4 h-4" />
          Thêm chương
        </button>
      </div>

      {isError ? (
        <div className="rounded-2xl border border-error/30 bg-error-container/20 p-6 text-center space-y-3">
          <p className="font-medium text-on-surface">Không thể tải danh sách chương.</p>
          <p className="text-sm text-on-surface-variant">{error instanceof Error ? error.message : 'Đã có lỗi xảy ra.'}</p>
          <button
            type="button"
            onClick={() => void refetch()}
            className="px-4 py-2 rounded-xl bg-primary text-on-primary text-sm font-medium hover:bg-secondary transition-colors"
          >
            Thử lại
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
          <div className="rounded-3xl border border-outline-variant/20 bg-surface p-4 shadow-sm space-y-4 h-fit">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-on-surface">Danh sách chương</p>
                <p className="text-xs text-on-surface-variant mt-1">{chapters.length} chương</p>
              </div>
              <BookOpen className="w-5 h-5 text-primary" />
            </div>

            <div className="space-y-2 max-h-[70vh] overflow-y-auto pr-1">
              {chapters.length > 0 ? (
                chapters.map((chapter) => {
                  const isActive = editingChapterId === chapter.id;
                  return (
                    <button
                      key={chapter.id}
                      type="button"
                      onClick={() => selectChapter(chapter)}
                      className={`w-full rounded-2xl border px-4 py-3 text-left transition-colors ${
                        isActive
                          ? 'border-primary bg-primary/10 text-on-surface'
                          : 'border-outline-variant/30 bg-surface-container-lowest text-on-surface hover:bg-surface-container-low'
                      }`}
                    >
                      <p className="text-sm font-medium">Chương {chapter.chapterNumber}</p>
                      <p className="mt-1 text-sm text-on-surface-variant line-clamp-2">{chapter.title}</p>
                    </button>
                  );
                })
              ) : (
                <div className="rounded-2xl border border-dashed border-outline-variant/30 px-4 py-6 text-center text-sm text-on-surface-variant">
                  Chưa có chương nào. Hãy tạo chương đầu tiên.
                </div>
              )}
            </div>
          </div>

          <div className="rounded-3xl border border-outline-variant/20 bg-surface p-6 shadow-sm">
            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 className="text-2xl font-serif font-semibold text-on-surface">
                    {editingChapterId ? 'Chỉnh sửa chương' : 'Thêm chương mới'}
                  </h2>
                  <p className="mt-1 text-sm text-on-surface-variant">Nhập nội dung chi tiết cho từng chương của sách.</p>
                </div>
                {editingChapterId && (
                  <button
                    type="button"
                    onClick={resetForm}
                    className="rounded-xl border border-outline-variant/40 px-4 py-2 text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors"
                  >
                    Tạo mới
                  </button>
                )}
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-on-surface">Số chương</label>
                  <input
                    type="number"
                    min={1}
                    value={form.chapterNumber}
                    onChange={(e) => setForm({ ...form, chapterNumber: Number(e.target.value) })}
                    className="w-full rounded-xl border border-outline-variant/50 bg-surface-container-lowest px-4 py-2.5 text-on-surface focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                  />
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-on-surface">Tiêu đề chương</label>
                  <input
                    type="text"
                    value={form.title}
                    onChange={(e) => setForm({ ...form, title: e.target.value })}
                    className="w-full rounded-xl border border-outline-variant/50 bg-surface-container-lowest px-4 py-2.5 text-on-surface focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                    placeholder="Ví dụ: Khởi đầu"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-on-surface">Nội dung</label>
                <textarea
                  rows={18}
                  value={form.content}
                  onChange={(e) => setForm({ ...form, content: e.target.value })}
                  className="w-full rounded-2xl border border-outline-variant/50 bg-surface-container-lowest px-4 py-3 text-sm text-on-surface focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 resize-y"
                  placeholder="Nhập nội dung chương..."
                />
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
                {editingChapterId && (
                  <button
                    type="button"
                    onClick={() => void handleDelete()}
                    disabled={isMutating}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-error/30 px-4 py-2.5 text-sm font-medium text-error hover:bg-error-container/40 transition-colors disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <Trash2 className="w-4 h-4" />
                    Xóa chương
                  </button>
                )}
                <button
                  type="submit"
                  disabled={isMutating}
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-medium text-on-primary transition-colors hover:bg-secondary disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <Save className="w-4 h-4" />
                  {isMutating ? 'Đang lưu...' : editingChapterId ? 'Lưu thay đổi' : 'Thêm chương'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
