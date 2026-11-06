import {AxiosInstance} from 'axios'

const API_SERVER = '/api/v1/server'
const API_ACTUATOR = '/actuator'

export default class ServerService {
  private http: AxiosInstance;

  constructor (http: AxiosInstance) {
    this.http = http
  }

  async optimizeAndRestart () {
    try {
      await this.http.post(`${API_SERVER}/optimize-and-restart`)
    } catch (e) {
      let msg = 'An error occurred while trying to optimize the databases and restart the server'
      if (e.response && e.response.data && e.response.data.message) {
        msg += `: ${e.response.data.message}`
      }
      throw new Error(msg)
    }
  }

  async isUp (): Promise<boolean> {
    try {
      const response = await this.http.get(`${API_ACTUATOR}/health`)
      return !!response.data && response.data.status === 'UP'
    } catch (e) {
      return false
    }
  }
}
