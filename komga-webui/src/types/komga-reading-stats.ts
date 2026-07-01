export interface TopSeriesReadingStatAggregateDto {
  seriesId: string,
  uniqueReaders: number,
  completedBooks: number,
  progressEvents: number,
  lastReadAt?: Date,
}
