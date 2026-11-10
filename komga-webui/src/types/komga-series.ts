import {AuthorDto, BookDto} from '@/types/komga-books'
import {Context} from '@/types/context'

export interface SeriesDto {
  id: string,
  libraryId: string,
  name: string,
  url: string,
  created: Date,
  lastModified: Date,
  booksCount: number,
  booksReadCount: number,
  booksUnreadCount: number,
  booksInProgressCount: number,
  metadata: SeriesMetadataDto,
  booksMetadata: SeriesBooksMetadataDto,
  deleted: boolean,
  oneshot: boolean,

  // custom fields
  context: Context
}

export interface SeriesMetadataDto {
  status: string,
  statusLock: boolean,
  created: string,
  lastModified: string,
  title: string,
  titleLock: boolean,
  titleSort: string,
  titleSortLock: boolean,
  summary: string,
  summaryLock: boolean,
  readingDirection: string,
  readingDirectionLock: boolean,
  genres: string[],
  genresLock: boolean,
  tags: string[],
  tagsLock: boolean,
}

export interface SeriesBooksMetadataDto {
  created: string,
  lastModified: string
  authors: AuthorDto[],
  tags: string[],
  releaseDate: string,
  summary: string,
  summaryNumber: string,
}

export interface SeriesMetadataUpdateDto {
  status?: string,
  statusLock?: boolean,
  title?: string,
  titleLock?: boolean,
  titleSort?: string,
  titleSortLock?: boolean,
  summary?: string,
  summaryLock?: boolean,
  readingDirection?: string,
  readingDirectionLock?: boolean,
  genres?: string[],
  genresLock?: boolean,
  tags?: string[],
  tagsLock?: boolean,
}

export interface GroupCountDto {
  group: string,
  count: number,
}

export interface SeriesThumbnailDto {
  id: string,
  seriesId: string,
  type: string,
  selected: boolean,
  mediaType: string,
  fileSize: number,
  width: number,
  height: number,
}


export interface Oneshot {
  series: SeriesDto,
  book: BookDto,
}
