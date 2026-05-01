import { useEffect, useState } from 'react'
import { api, type ProfileResponse, setAuth } from '../api'
import { useAuth } from '../auth-context'
import { ErrorBanner } from '../ui'

export function ProfilePage() {
  const { me, setSession } = useAuth()

  // Profile data
  const [profile, setProfile] = useState<ProfileResponse | null>(null)
  const [loadErr, setLoadErr] = useState<string | null>(null)

  // Edit fields
  const [name, setName] = useState('')
  const [bio, setBio] = useState('')
  const [profileBusy, setProfileBusy] = useState(false)
  const [profileErr, setProfileErr] = useState<string | null>(null)
  const [profileOk, setProfileOk] = useState(false)

  // Password fields
  const [currentPw, setCurrentPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [confirmPw, setConfirmPw] = useState('')
  const [pwBusy, setPwBusy] = useState(false)
  const [pwErr, setPwErr] = useState<string | null>(null)
  const [pwOk, setPwOk] = useState(false)

  useEffect(() => {
    api.getProfile()
      .then((p) => {
        setProfile(p)
        setName(p.name)
        setBio(p.bio ?? '')
      })
      .catch((e) => setLoadErr(e instanceof Error ? e.message : 'Failed to load profile'))
  }, [])

  async function saveProfile(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) { setProfileErr('Name is required.'); return }
    setProfileBusy(true)
    setProfileErr(null)
    setProfileOk(false)
    try {
      const updated = await api.updateProfile({ name: name.trim(), bio: bio.trim() || undefined })
      setProfile(updated)
      setName(updated.name)
      setBio(updated.bio ?? '')
      // Sync name in auth context / localStorage
      if (me) setSession({ ...me, name: updated.name })
      setProfileOk(true)
      setTimeout(() => setProfileOk(false), 3000)
    } catch (e) {
      setProfileErr(e instanceof Error ? e.message : 'Failed to update profile')
    } finally {
      setProfileBusy(false)
    }
  }

  async function savePassword(e: React.FormEvent) {
    e.preventDefault()
    if (newPw.length < 6) { setPwErr('New password must be at least 6 characters.'); return }
    if (newPw !== confirmPw) { setPwErr('Passwords do not match.'); return }
    setPwBusy(true)
    setPwErr(null)
    setPwOk(false)
    try {
      await api.changePassword(currentPw, newPw)
      setCurrentPw('')
      setNewPw('')
      setConfirmPw('')
      setPwOk(true)
      setTimeout(() => setPwOk(false), 3000)
    } catch (e) {
      setPwErr(e instanceof Error ? e.message : 'Failed to change password')
    } finally {
      setPwBusy(false)
    }
  }

  // Avatar initials
  const initials = (profile?.name ?? me?.name ?? '?').slice(0, 2).toUpperCase()

  // Member since
  const memberSince = profile?.createdAt
    ? new Date(profile.createdAt).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
    : '—'

  return (
    <div className="profilePage">
      {/* Cover banner */}
      <div className="profileCover" aria-hidden="true" />

      {/* Avatar + name header */}
      <div className="profileHeader">
        <div className="profileAvatarWrap">
          <div className="profileAvatar" aria-label={`Avatar for ${profile?.name}`}>
            {initials}
          </div>
        </div>
        <div className="profileHeaderInfo">
          <div className="profileName">{profile?.name ?? '—'}</div>
          <div className="profileEmail muted small">{profile?.email ?? '—'}</div>
          {profile?.bio ? <div className="profileBio muted">{profile.bio}</div> : null}
          <div className="muted small">Member since {memberSince}</div>
        </div>
      </div>

      <ErrorBanner message={loadErr} />

      <div className="profileBody">
        {/* Left: edit profile */}
        <div className="profileSection">
          <div className="profileSectionTitle">My Details</div>

          <form className="form" onSubmit={saveProfile}>
            <div className="row2">
              <label className="field">
                <div className="label">First name</div>
                <input
                  value={name.split(' ')[0] ?? ''}
                  onChange={(e) => {
                    const parts = name.split(' ')
                    parts[0] = e.target.value
                    setName(parts.join(' ').trim())
                  }}
                  placeholder="First name"
                />
              </label>
              <label className="field">
                <div className="label">Last name</div>
                <input
                  value={name.split(' ').slice(1).join(' ')}
                  onChange={(e) => {
                    const first = name.split(' ')[0] ?? ''
                    setName(e.target.value ? `${first} ${e.target.value}` : first)
                  }}
                  placeholder="Last name"
                />
              </label>
            </div>

            <label className="field">
              <div className="label">Email</div>
              <div className="profileEmailField">
                <span className="profileEmailIcon">✉</span>
                <input value={profile?.email ?? ''} disabled readOnly />
              </div>
            </label>

            <label className="field">
              <div className="label">Bio (optional)</div>
              <textarea
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                rows={3}
                placeholder="Tell your team a little about yourself…"
                maxLength={300}
              />
              <div className="profileCharCount muted small">{bio.length}/300</div>
            </label>

            {profileErr ? <div className="error">{profileErr}</div> : null}
            {profileOk ? <div className="profileSuccess">Profile saved ✓</div> : null}

            <div className="profileFormActions">
              <button
                type="button"
                className="btn subtle"
                onClick={() => { setName(profile?.name ?? ''); setBio(profile?.bio ?? '') }}
              >
                Cancel
              </button>
              <button className="btn" type="submit" disabled={profileBusy || !name.trim()}>
                {profileBusy ? 'Saving…' : 'Save'}
              </button>
            </div>
          </form>
        </div>

        {/* Right: change password */}
        <div className="profileSection">
          <div className="profileSectionTitle">Change Password</div>

          <form className="form" onSubmit={savePassword}>
            <label className="field">
              <div className="label">Current password</div>
              <input
                type="password"
                value={currentPw}
                onChange={(e) => setCurrentPw(e.target.value)}
                placeholder="••••••••"
              />
            </label>
            <label className="field">
              <div className="label">New password</div>
              <input
                type="password"
                value={newPw}
                onChange={(e) => setNewPw(e.target.value)}
                placeholder="Min. 6 characters"
              />
            </label>
            <label className="field">
              <div className="label">Confirm new password</div>
              <input
                type="password"
                value={confirmPw}
                onChange={(e) => setConfirmPw(e.target.value)}
                placeholder="Repeat new password"
              />
            </label>

            {pwErr ? <div className="error">{pwErr}</div> : null}
            {pwOk ? <div className="profileSuccess">Password changed ✓</div> : null}

            <button className="btn" type="submit" disabled={pwBusy || !currentPw || !newPw || !confirmPw}>
              {pwBusy ? 'Updating…' : 'Update password'}
            </button>
          </form>

          {/* Account info card */}
          <div className="profileInfoCard">
            <div className="profileInfoCardTitle muted small">Account info</div>
            <div className="profileInfoRow">
              <span className="muted small">User ID</span>
              <span className="profileInfoValue small">{profile?.id ?? '—'}</span>
            </div>
            <div className="profileInfoRow">
              <span className="muted small">Member since</span>
              <span className="small">{memberSince}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
