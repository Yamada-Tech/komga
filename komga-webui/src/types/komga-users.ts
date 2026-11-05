import {AllowExclude} from '@/types/enum-users'

export interface UserDto {
  id: string,
  email: string,
  roles: string[],
  sharedAllLibraries: boolean,
  sharedLibrariesIds: string[],
  labelsAllow: string[],
  labelsExclude: string[],
  ageRestriction?: {
    age: number,
    restriction: AllowExclude,
  },
  lastLoginDate?: Date,
}

export interface UserCreationDto {
  email: string,
  roles: string[]
}

export interface PasswordUpdateDto {
  password: string
}

export interface UserUpdateDto {
  roles?: string[],
  sharedLibraries?: {
    all: boolean,
    libraryIds: string[]
  },
  ageRestriction?: {
    age: number,
    restriction: AllowExclude,
  }
  labelsAllow?: string[],
  labelsExclude?: string[],
}

export interface RecentReadDto {
  userId: string,
  userEmail: string,
  bookId: string,
  bookTitle: string,
  seriesId: string,
  seriesTitle: string,
  page: number,
  pagesCount?: number,
  completed: boolean,
  readDate: Date,
}

export interface ApiKeyDto {
  id: string,
  userId: string,
  key: string,
  comment: string,
  createdDate: Date,
  lastModifiedDate: Date
}

export interface ApiKeyRequestDto {
  comment: string,
}
