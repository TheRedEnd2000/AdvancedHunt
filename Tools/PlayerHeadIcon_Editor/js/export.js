export class ExportManager {
    constructor(canvasManager) {
        this.canvasManager = canvasManager;
    }

    exportSkin() {
        const dataCanvas = this.canvasManager.getDataCanvas();
        const link = document.createElement('a');
        link.download = 'minecraft_skin.png';
        link.href = dataCanvas.toDataURL();
        link.click();
    }

    exportHeadIcon() {
        const dataCanvas = this.canvasManager.getDataCanvas();
        const size = 512;
        const iconCanvas = document.createElement('canvas');
        iconCanvas.width = size;
        iconCanvas.height = size;
        const ctx = iconCanvas.getContext('2d');
        ctx.imageSmoothingEnabled = false;

        const cx = size / 2;
        const cy = size / 2 + 40;
        const scale = 24;

        const drawLayer = (sx, sy, m, isHat) => {
            ctx.save();

            if (isHat) {
                const centerX = cx;
                const centerY = cy + 2 * scale;

                ctx.translate(centerX, centerY);
                ctx.scale(1.15, 1.15);
                ctx.translate(-centerX, -centerY);
            }

            ctx.translate(cx, cy);
            ctx.transform(...m);
            ctx.drawImage(dataCanvas, sx, sy, 8, 8, 0, 0, 8 * scale, 8 * scale);
            ctx.restore();
        };

        // Transformation matrices
        const mLeft = [1, 0.5, 0, 1.118, -8 * scale, -4 * scale];
        const mRight = [1, -0.5, 0, 1.118, 0, 0];
        const mTop = [1, -0.5, 1, 0.5, -8 * scale, -4 * scale];

        // Draw Inner Head
        drawLayer(8, 0, mTop, false);    // Top
        drawLayer(0, 8, mLeft, false);   // Visual Left -> Right Texture
        drawLayer(8, 8, mRight, false);  // Visual Right -> Front Texture

        // Draw Outer Hat
        if (document.getElementById('toggle-hat').checked) {
            drawLayer(40, 0, mTop, true);   // Top Hat
            drawLayer(40, 8, mRight, true); // Front Hat
            drawLayer(32, 8, mLeft, true);  // Right Hat
        }

        const link = document.createElement('a');
        link.download = 'head_icon.png';
        link.href = iconCanvas.toDataURL();
        link.click();
    }
}
