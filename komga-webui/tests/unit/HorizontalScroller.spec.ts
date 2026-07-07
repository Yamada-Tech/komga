import HorizontalScroller from '@/components/HorizontalScroller.vue'

describe('HorizontalScroller E-Ink behavior', () => {
  const methods = (HorizontalScroller as any).options.methods

  it('uses immediate scroll behavior when E-Ink mode is enabled', () => {
    const scrollTo = jest.fn()
    const context = {
      container: {
        clientWidth: 500,
        scrollLeft: 0,
        scrollTo,
      },
      adjustment: 100,
      einkMode: true,
      $vuetify: {
        rtl: false,
      },
    }

    methods.doScroll.call(context, 'forward')
    expect(scrollTo).toHaveBeenCalledWith(expect.objectContaining({behavior: 'auto'}))
  })

  it('prevents touch scrolling when E-Ink mode is enabled', () => {
    const event = {
      preventDefault: jest.fn(),
    } as unknown as TouchEvent
    const context = {
      einkMode: true,
    }

    methods.preventTouchScroll.call(context, event)
    expect(event.preventDefault).toHaveBeenCalled()
  })

  it('does not prevent touch scrolling outside E-Ink mode', () => {
    const event = {
      preventDefault: jest.fn(),
    } as unknown as TouchEvent
    const context = {
      einkMode: false,
    }

    methods.preventTouchScroll.call(context, event)
    expect(event.preventDefault).not.toHaveBeenCalled()
  })
})
