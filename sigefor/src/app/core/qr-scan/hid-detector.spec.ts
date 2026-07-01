import { HidDetector } from './hid-detector';

describe('HidDetector', () => {
  /** Reloj controlable para simular la cadencia de pulsaciones. */
  function detectorWithClock(): { detector: HidDetector; advance: (ms: number) => void } {
    let clock = 1000;
    const detector = new HidDetector(
      { maxKeystrokeGapMs: 35, minLength: 3 },
      () => clock,
    );
    return { detector, advance: (ms) => (clock += ms) };
  }

  it('reconoce una ráfaga rápida terminada en Enter como lector HID', () => {
    const { detector, advance } = detectorWithClock();
    for (const key of ['F', 'O', 'R', '1', '2']) {
      expect(detector.push(key)).toBeNull();
      advance(10);
    }
    expect(detector.push('Enter')).toBe('FOR12');
  });

  it('ignora el tecleo humano lento (huecos grandes entre teclas)', () => {
    const { detector, advance } = detectorWithClock();
    detector.push('F');
    advance(300);
    detector.push('O');
    advance(300);
    detector.push('R');
    advance(300);
    // Al terminar, la ráfaga quedó rota por los huecos → no se reconoce como lector.
    expect(detector.push('Enter')).toBeNull();
  });

  it('descarta ráfagas demasiado cortas', () => {
    const { detector, advance } = detectorWithClock();
    detector.push('A');
    advance(10);
    detector.push('B');
    expect(detector.push('Enter')).toBeNull();
  });
});
