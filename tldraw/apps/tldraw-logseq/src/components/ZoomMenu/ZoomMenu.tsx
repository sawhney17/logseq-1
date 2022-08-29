import * as DropdownMenuPrimitive from '@radix-ui/react-dropdown-menu'
import { useApp } from '@tldraw/react'
import { MOD_KEY} from '@tldraw/core'
import { observer } from 'mobx-react-lite'

export const ZoomMenu = observer(function ZoomMenu(): JSX.Element {
  const app = useApp()
  const preventEvent = (e: Event) => {
    e.preventDefault()
  }
  return (
    <DropdownMenuPrimitive.Root>
      <DropdownMenuPrimitive.Trigger>
        {(app.viewport.camera.zoom * 100).toFixed(0) + '%'}
      </DropdownMenuPrimitive.Trigger>
      <DropdownMenuPrimitive.Content
        className="tl-zoom-menu-dropdown-menu-button"
        id="zoomPopup"
        sideOffset={12}
      >
        <DropdownMenuPrimitive.Arrow style={{ fill: 'white' }}></DropdownMenuPrimitive.Arrow>
        <DropdownMenuPrimitive.Item
          className="menu-link tl-zoom-menu-dropdown-item"
          onSelect={preventEvent}
          onClick={app.api.zoomToFit}
        >
          Zoom to Fit <div className="tl-zoom-menu-right-slot"></div>
        </DropdownMenuPrimitive.Item>
        <DropdownMenuPrimitive.Item
          className="menu-link tl-zoom-menu-dropdown-item"
          onSelect={preventEvent}
          onClick={app.api.zoomToSelection}
        >
          Zoom to Selection <div className="tl-zoom-menu-right-slot"><span className="keyboard-shortcut"><code>{MOD_KEY}</code> <code>-</code></span></div>
        </DropdownMenuPrimitive.Item>
        <DropdownMenuPrimitive.Item
          className="menu-link tl-zoom-menu-dropdown-item"
          onSelect={preventEvent}
          onClick={app.api.zoomIn}
        >
          Zoom In <div className="tl-zoom-menu-right-slot"><span className="keyboard-shortcut"><code>{MOD_KEY}</code> <code>+</code></span></div>
        </DropdownMenuPrimitive.Item>
        <DropdownMenuPrimitive.Item
          className="menu-link tl-zoom-menu-dropdown-item"
          onSelect={preventEvent}
          onClick={app.api.zoomOut}
        >
          Zoom Out <div className="tl-zoom-menu-right-slot"><span className="keyboard-shortcut"><code>{MOD_KEY}</code> <code>-</code></span></div>
        </DropdownMenuPrimitive.Item>
        <DropdownMenuPrimitive.Item
          className="menu-link tl-zoom-menu-dropdown-item"
          onSelect={preventEvent}
          onClick={app.api.resetZoom}
        >
          Reset Zoom <div className="tl-zoom-menu-right-slot"><span className="keyboard-shortcut"><code>⇧</code> <code>0</code></span></div>
        </DropdownMenuPrimitive.Item>
      </DropdownMenuPrimitive.Content>
    </DropdownMenuPrimitive.Root>
  )
})

export default ZoomMenu