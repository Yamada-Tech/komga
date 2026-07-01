import axios, {AxiosInstance} from 'axios'
import {TopSeriesReadingStatAggregateDto} from '@/types/komga-reading-stats'

const API_READING_STATS = '/api/v1/reading-stats'

export default class KomgaReadingStatsService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  async getTopSeriesByPeriod(period: string = 'weekly', limit: number = 10): Promise<TopSeriesReadingStatAggregateDto[]> {
    try {
      return (await this.http.get(`${API_READING_STATS}/series/top-by-period`, {
        params: {period, limit},
      })).data
    } catch (e) {
      let msg = 'An error occurred while trying to retrieve top series by period'
      if (axios.isAxiosError(e) && e.response?.data?.message) {
        msg += `: ${e.response.data.message}`
      } else if (e instanceof Error && e.message) {
        msg += `: ${e.message}`
      }
      throw new Error(msg)
    }
  }
}
