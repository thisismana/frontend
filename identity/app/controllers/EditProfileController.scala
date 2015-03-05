package controllers

import actions.AuthenticatedActions
import actions.AuthenticatedActions.AuthRequest
import com.google.inject.{Inject, Singleton}
import com.gu.identity.model.User
import common.{ExecutionContexts, JsonComponent}
import conf.Configuration
import form._
import idapiclient.IdApiClient
import model._
import play.api.data.Form
import play.api.mvc.{AnyContent, Controller, Request}
import play.filters.csrf.{CSRFAddToken, CSRFCheck}
import services.{IdentityRequest, _}
import tracking.{Omniture, TrackingParams}
import utils.SafeLogging

import scala.concurrent.Future

@Singleton
class EditProfileController @Inject()(idUrlBuilder: IdentityUrlBuilder,
                                      authenticatedActions: AuthenticatedActions,
                                      identityApiClient: IdApiClient,
                                      idRequestParser: IdRequestParser)
  extends Controller
  with ExecutionContexts
  with SafeLogging{

  import authenticatedActions._

  type OmniPage = IdentityPage with Omniture

  protected val accountPage = IdentityPage("/account/edit", "Edit Account Details", "edit account details")
  protected val publicPage = IdentityPage("/public/edit", "Edit Public Profile", "edit public profile")
  protected val membershipPage = IdentityPage("/membership/edit", "Membership", "edit membership details")

  lazy val AvatarSigningService = new AvatarSigningService(Configuration.avatars.signingKey)

  def displayPublicProfileForm = displayForm(publicPage)
  def displayAccountForm = displayForm(accountPage)
  def displayMembershipForm = displayForm(membershipPage)

  protected def displayForm(page: OmniPage) = CSRFAddToken {
    recentlyAuthenticated.async { implicit request =>
      profileFormsView(page.tracking, ProfileForms(request.user, false))
    }
  }

  def submitPublicProfileForm() = submitForm(publicPage)
  def submitAccountForm() = submitForm(accountPage)

  def submitForm(page: OmniPage) = CSRFCheck {
    authActionWithUser.async {
      implicit request =>
        val idRequest = idRequestParser(request)
        val user = request.user
        val forms = ProfileForms(user, page == publicPage).bindFromRequest(request)
        val futureFormOpt = forms.activeForm.value map {
          data: UserFormData =>
            identityApiClient.saveUser(user.id, data.toUserUpdate(user), user.auth) map {
              case Left(errors) =>
                forms.withErrors(errors)

              case Right(user) => forms.bindForms(user)
            }
        }

        val futureForms = futureFormOpt getOrElse Future.successful(forms)
        futureForms flatMap {
          forms =>
            profileFormsView(page.accountEdited, forms)
        }
    }
  }

  def profileFormsView(pageWithTrackingParamsFor: IdentityRequest => IdentityPage with TrackingParams, forms: ProfileForms)(implicit request: AuthRequest[AnyContent]) = {
    val idRequest = idRequestParser(request)
    val user = request.user

    val avatarUploadStatus = for {
      responseData <- request.queryString get "signed_data"
      signedString <- responseData.headOption
    } yield AvatarSigningService wasUploadSuccessful signedString

    Future(NoCache(Ok(views.html.profileForms(
           pageWithTrackingParamsFor(idRequest),
           user, forms, idRequest, idUrlBuilder,
           Some(avatarUploadDataFor(user)),
           avatarUploadStatus))))
  }

  private def avatarUploadDataFor(user: User) = AvatarUploadData(AvatarSigningService.sign(AvatarData(user)))
}

case class ProfileForms(publicForm: Form[ProfileFormData], accountForm: Form[AccountFormData], isPublicFormActive: Boolean)
  extends ProfileMapping
  with AccountDetailsMapping {

  lazy val activeForm = if(isPublicFormActive) publicForm else accountForm

  def bindFromRequest(implicit request: Request[_]) = update {
    form =>
      // Hack to get the postcode error into the correct context.
      val boundForm = form.bindFromRequest()
      boundForm.error("address") map {
        e =>
          boundForm.withError(e.copy(key = "address.postcode"))
      } getOrElse boundForm
  }

  def bindForms(user: User): ProfileForms = {
    copy(
      publicForm = profileMapping.bindForm(user),
      accountForm = accountDetailsMapping.bindForm(user)
    )
  }

  def withErrors(errors: List[client.Error]): ProfileForms = {
    update{
      form =>
        errors.foldLeft(form){
          (formWithErrors, error) =>
            val context = activeMapping.mapContext(error.context getOrElse "")
            formWithErrors.withError(context, error.description)
        }
    }
  }

  private lazy val activeMapping = if(isPublicFormActive) profileMapping else accountDetailsMapping

  private def update(change: (Form[_ <: UserFormData]) => Form[_ <: UserFormData]): ProfileForms = {
    if(isPublicFormActive){
      copy(publicForm = change(publicForm).asInstanceOf[Form[ProfileFormData]])
    }
    else
      copy(accountForm = change(accountForm).asInstanceOf[Form[AccountFormData]])
  }
}

object ProfileForms
  extends ProfileMapping
  with AccountDetailsMapping {

  def apply(user: User, isPublicFormActive: Boolean): ProfileForms = ProfileForms(
    publicForm = profileMapping.bindForm(user),
    accountForm = accountDetailsMapping.bindForm(user),
    isPublicFormActive = isPublicFormActive
  )
}
