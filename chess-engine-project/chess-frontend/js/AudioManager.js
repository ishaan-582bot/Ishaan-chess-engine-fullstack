/**
 * AudioManager - Sound effects for chess moves
 * 
 * Features:
 * - Web Audio API synthesis (no external files needed)
 * - Different sounds for: move, capture, castle, check, promotion, game end
 * - Volume control
 * - Enable/disable toggle
 * 
 * @author Chess Frontend Developer
 * @version 1.0.0
 */

class AudioManager {
    constructor() {
        this.enabled = true;
        this.volume = 0.5;
        this.context = null;
        this.initialized = false;
    }

    /**
     * Initialize audio context (must be called after user interaction)
     */
    init() {
        if (this.initialized) return;
        
        try {
            this.context = new (window.AudioContext || window.webkitAudioContext)();
            this.initialized = true;
        } catch (error) {
            console.warn('Web Audio API not supported');
        }
    }

    /**
     * Set audio enabled state
     * @param {boolean} enabled
     */
    setEnabled(enabled) {
        this.enabled = enabled;
    }

    /**
     * Set volume level
     * @param {number} volume - 0.0 to 1.0
     */
    setVolume(volume) {
        this.volume = Math.max(0, Math.min(1, volume));
    }

    /**
     * Play a sound if enabled
     * @param {Function} soundGenerator - Function that generates the sound
     */
    play(soundGenerator) {
        if (!this.enabled || !this.initialized || !this.context) return;
        
        try {
            soundGenerator();
        } catch (error) {
            console.warn('Audio playback failed:', error);
        }
    }

    /**
     * Play move sound (short click)
     */
    playMove() {
        this.play(() => {
            const osc = this.context.createOscillator();
            const gain = this.context.createGain();
            
            osc.connect(gain);
            gain.connect(this.context.destination);
            
            osc.type = 'sine';
            osc.frequency.setValueAtTime(800, this.context.currentTime);
            osc.frequency.exponentialRampToValueAtTime(400, this.context.currentTime + 0.1);
            
            gain.gain.setValueAtTime(this.volume * 0.3, this.context.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, this.context.currentTime + 0.1);
            
            osc.start(this.context.currentTime);
            osc.stop(this.context.currentTime + 0.1);
        });
    }

    /**
     * Play capture sound (wooden thud)
     */
    playCapture() {
        this.play(() => {
            // Create a noise burst for the "thud" sound
            const bufferSize = this.context.sampleRate * 0.1;
            const buffer = this.context.createBuffer(1, bufferSize, this.context.sampleRate);
            const data = buffer.getChannelData(0);
            
            for (let i = 0; i < bufferSize; i++) {
                data[i] = Math.random() * 2 - 1;
            }
            
            const noise = this.context.createBufferSource();
            noise.buffer = buffer;
            
            const filter = this.context.createBiquadFilter();
            filter.type = 'lowpass';
            filter.frequency.value = 1000;
            
            const gain = this.context.createGain();
            gain.gain.setValueAtTime(this.volume * 0.5, this.context.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, this.context.currentTime + 0.15);
            
            noise.connect(filter);
            filter.connect(gain);
            gain.connect(this.context.destination);
            
            noise.start(this.context.currentTime);
            noise.stop(this.context.currentTime + 0.15);
        });
    }

    /**
     * Play castle sound (sliding sound)
     */
    playCastle() {
        this.play(() => {
            const osc = this.context.createOscillator();
            const gain = this.context.createGain();
            
            osc.connect(gain);
            gain.connect(this.context.destination);
            
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(600, this.context.currentTime);
            osc.frequency.linearRampToValueAtTime(800, this.context.currentTime + 0.2);
            
            gain.gain.setValueAtTime(this.volume * 0.2, this.context.currentTime);
            gain.gain.linearRampToValueAtTime(0.01, this.context.currentTime + 0.2);
            
            osc.start(this.context.currentTime);
            osc.stop(this.context.currentTime + 0.2);
        });
    }

    /**
     * Play check sound (alert tone)
     */
    playCheck() {
        this.play(() => {
            const osc = this.context.createOscillator();
            const gain = this.context.createGain();
            
            osc.connect(gain);
            gain.connect(this.context.destination);
            
            osc.type = 'square';
            osc.frequency.setValueAtTime(880, this.context.currentTime);
            osc.frequency.setValueAtTime(1100, this.context.currentTime + 0.1);
            
            gain.gain.setValueAtTime(this.volume * 0.3, this.context.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, this.context.currentTime + 0.3);
            
            osc.start(this.context.currentTime);
            osc.stop(this.context.currentTime + 0.3);
        });
    }

    /**
     * Play promotion sound (fanfare)
     */
    playPromotion() {
        this.play(() => {
            const osc = this.context.createOscillator();
            const gain = this.context.createGain();
            
            osc.connect(gain);
            gain.connect(this.context.destination);
            
            osc.type = 'sine';
            osc.frequency.setValueAtTime(523.25, this.context.currentTime); // C5
            osc.frequency.setValueAtTime(659.25, this.context.currentTime + 0.1); // E5
            osc.frequency.setValueAtTime(783.99, this.context.currentTime + 0.2); // G5
            
            gain.gain.setValueAtTime(this.volume * 0.3, this.context.currentTime);
            gain.gain.linearRampToValueAtTime(0.01, this.context.currentTime + 0.4);
            
            osc.start(this.context.currentTime);
            osc.stop(this.context.currentTime + 0.4);
        });
    }

    /**
     * Play game end sound
     * @param {boolean} win - Whether player won
     */
    playGameEnd(win) {
        this.play(() => {
            const osc = this.context.createOscillator();
            const gain = this.context.createGain();
            
            osc.connect(gain);
            gain.connect(this.context.destination);
            
            if (win) {
                // Victory fanfare (ascending)
                osc.type = 'sine';
                osc.frequency.setValueAtTime(440, this.context.currentTime);
                osc.frequency.setValueAtTime(554, this.context.currentTime + 0.15);
                osc.frequency.setValueAtTime(659, this.context.currentTime + 0.3);
                osc.frequency.setValueAtTime(880, this.context.currentTime + 0.45);
            } else {
                // Defeat sound (descending)
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(440, this.context.currentTime);
                osc.frequency.setValueAtTime(349, this.context.currentTime + 0.2);
                osc.frequency.setValueAtTime(293, this.context.currentTime + 0.4);
            }
            
            gain.gain.setValueAtTime(this.volume * 0.4, this.context.currentTime);
            gain.gain.linearRampToValueAtTime(0.01, this.context.currentTime + 0.6);
            
            osc.start(this.context.currentTime);
            osc.stop(this.context.currentTime + 0.6);
        });
    }

    /**
     * Play illegal move sound (error buzz)
     */
    playIllegal() {
        this.play(() => {
            const osc = this.context.createOscillator();
            const gain = this.context.createGain();
            
            osc.connect(gain);
            gain.connect(this.context.destination);
            
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(150, this.context.currentTime);
            
            gain.gain.setValueAtTime(this.volume * 0.2, this.context.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, this.context.currentTime + 0.15);
            
            osc.start(this.context.currentTime);
            osc.stop(this.context.currentTime + 0.15);
        });
    }
}

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { AudioManager };
}
