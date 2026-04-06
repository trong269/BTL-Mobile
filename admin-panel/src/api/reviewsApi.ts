import { axiosClient } from './axiosClient';

interface BackendReview {
  id: string;
  userId: string;
  bookId: string;
  rating: number;
  review: string;
  createdAt: string;
}

export interface ReviewDto {
  id: string;
  user: string;
  rating: number;
  comment: string;
  date: string;
}

export interface ReviewPayload {
  bookId: string;
  userId: string;
  rating: number;
  review: string;
}

function mapReview(review: BackendReview): ReviewDto {
  return {
    id: review.id,
    user: review.userId,
    rating: review.rating,
    comment: review.review,
    date: new Date(review.createdAt).toISOString().split('T')[0],
  };
}

export async function getBookReviews(bookId: string) {
  const response = await axiosClient.get<BackendReview[]>(`/reviews/book/${bookId}`);
  return response.data.map(mapReview);
}

export async function addReview(payload: ReviewPayload) {
  const response = await axiosClient.post<BackendReview>('/reviews', payload);
  return mapReview(response.data);
}
