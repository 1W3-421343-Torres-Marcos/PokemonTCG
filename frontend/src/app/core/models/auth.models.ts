export interface UserProfile {
  id: string;
  username: string;
  email: string;
  role: string;
  matchesPlayed: number;
  matchesWon: number;
  favoriteCardIds: string[];
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
